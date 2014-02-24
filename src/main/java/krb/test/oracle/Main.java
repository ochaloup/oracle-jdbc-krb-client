package krb.test.oracle;

import oracle.jdbc.OracleConnection;
import oracle.jdbc.OracleDriver;
import oracle.net.ano.AnoServices;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

/**
 * @author Martin Simka
 */
public class Main {
    //oracle12c
    private static final String JDBC_URL = "jdbc:oracle:thin:@dev151.mw.lab.eng.bos.redhat.com:1521:qaora12";
    //oracle11gR1
    //private static final String JDBC_URL = "jdbc:oracle:thin:@db03.mw.lab.eng.bos.redhat.com:1521:qaora11";
    //oracle11gR2
    //private static final String JDBC_URL = "jdbc:oracle:thin:@db04.mw.lab.eng.bos.redhat.com:1521:qaora11";

    public static void main(String[] args) throws Exception {
        System.setProperty("oracle.jdbc.Trace", "true");
        System.setProperty("sun.security.krb5.debug", "true");

        Properties props = new Properties();
        props.setProperty(OracleConnection.CONNECTION_PROPERTY_THIN_NET_AUTHENTICATION_SERVICES,
                "(" + AnoServices.AUTHENTICATION_KERBEROS5 + ")");
        props.setProperty(OracleConnection.CONNECTION_PROPERTY_THIN_NET_AUTHENTICATION_KRB5_MUTUAL,
                "true");
        props.setProperty("user", "KRBUSR01");


        DriverManager.registerDriver(new OracleDriver());
        Connection conn = DriverManager.getConnection(JDBC_URL, props);
        String auth = ((OracleConnection) conn).getAuthenticationAdaptorName();
        System.out.println("Authentication adaptor=" + auth);

        printUserName(conn);

        conn.close();
    }

    private static void printUserName(Connection conn) throws SQLException {
        Statement stmt = null;
        try {
            stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("select user from dual");
            while (rs.next())
                System.out.println("User is: " + rs.getString(1));
            rs.close();
        } finally {
            if (stmt != null)
                stmt.close();
        }
    }
}
