package krb.test.oracle;

import oracle.jdbc.OracleConnection;
import oracle.jdbc.OracleDriver;
import oracle.net.ano.AnoServices;

import javax.security.auth.Subject;
import javax.security.auth.login.AppConfigurationEntry;
import javax.security.auth.login.Configuration;
import javax.security.auth.login.LoginContext;
import java.io.File;
import java.security.PrivilegedExceptionAction;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
 * @author Martin Simka
 */
public class Main {
    private static final Logger logger = Logger.getLogger(Main.class.getName());
    //oracle12c
//    private static final String JDBC_URL = "jdbc:oracle:thin:@dev151.mw.lab.eng.bos.redhat.com:1521:qaora12";
    //oracle11gR1
    //private static final String JDBC_URL = "jdbc:oracle:thin:@db03.mw.lab.eng.bos.redhat.com:1521:qaora11";
    //oracle11gR2
    private static final String JDBC_URL = "jdbc:oracle:thin:@db04.mw.lab.eng.bos.redhat.com:1521:qaora11";

    public static void main(String[] args) throws Exception {
        Logger oracleLogger = Logger.getLogger("oracle.jdbc");
        oracleLogger.setLevel(Level.ALL);
        FileHandler handler = new FileHandler("log.txt", false);
        handler.setFormatter(new SimpleFormatter());
        handler.setLevel(Level.ALL);
        logger.addHandler(handler);
        oracleLogger.addHandler(handler);

        System.setProperty("oracle.jdbc.Trace", "true");
        System.setProperty("sun.security.krb5.debug", "true");
        System.setProperty("java.security.krb5.realm", "MW.LAB.ENG.BOS.REDHAT.COM");
        System.setProperty("java.security.krb5.kdc", "kerberos-test.mw.lab.eng.bos.redhat.com");
        System.setProperty("java.security.krb5.conf", new File("krb5.conf").getAbsolutePath());

        class Krb5LoginConfiguration extends Configuration {

            private final AppConfigurationEntry[] configList = new AppConfigurationEntry[1];

            public Krb5LoginConfiguration() {
                Map<String, String> options = new HashMap<String, String>();
                options.put("storeKey", "false");
                options.put("useKeyTab", "true");
                options.put("keyTab", "KRBUSR01");
                options.put("principal", "KRBUSR01@MW.LAB.ENG.BOS.REDHAT.COM");
                options.put("doNotPrompt", "true");
                options.put("useTicketCache", "true");
                options.put("ticketCache", "/tmp/krbcc_1000");
                options.put("refreshKrb5Config", "true");
                options.put("isInitiator", "true");
                options.put("addGSSCredential", "true");
                configList[0] = new AppConfigurationEntry(
                        "org.jboss.security.negotiation.KerberosLoginModule",
                        AppConfigurationEntry.LoginModuleControlFlag.REQUIRED,
                        options);
            }

            @Override
            public AppConfigurationEntry[] getAppConfigurationEntry(String name) {
                return configList;
            }
        }

        Configuration.setConfiguration(new Krb5LoginConfiguration());
        final LoginContext lc = new LoginContext("test");
        lc.login();
        Subject subject = lc.getSubject();

        final Properties props = new Properties();
        props.setProperty(OracleConnection.CONNECTION_PROPERTY_THIN_NET_AUTHENTICATION_SERVICES,
                "(" + AnoServices.AUTHENTICATION_KERBEROS5 + ")");
        props.setProperty(OracleConnection.CONNECTION_PROPERTY_THIN_NET_AUTHENTICATION_KRB5_MUTUAL,
                "true");

        try {
            Connection conn = Subject.doAs(subject, new PrivilegedExceptionAction<Connection>() {
                @Override
                public Connection run() throws Exception {
                    return DriverManager.getConnection(JDBC_URL, props);
                }
            });

            printUserName(conn);
            conn.close();
        } catch (Throwable t) {
            logger.log(Level.SEVERE, "", t);
        }
    }

    private static void printUserName(Connection conn) throws SQLException {
        Statement stmt = null;
        try {
            stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("select user from dual");
            while (rs.next()) {
                String user = rs.getString(1);
                System.out.println("User is: " + user);
                logger.log(Level.INFO, "User is: " + user);
            }
            rs.close();
        } finally {
            if (stmt != null)
                stmt.close();
        }
    }
}
