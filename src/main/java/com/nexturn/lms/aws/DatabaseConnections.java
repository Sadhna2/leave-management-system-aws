package com.nexturn.lms.aws;
import javax.sql.DataSource;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
/** Creates a minimal JDBC connection factory for infrastructure handlers without starting the web app. */
public final class DatabaseConnections {
    private DatabaseConnections() { }
    public static DataSource create(String usernameKey,String passwordKey) {
        DriverManagerDataSource source=new DriverManagerDataSource();
        source.setDriverClassName("com.mysql.cj.jdbc.Driver");
        source.setUrl(required("DB_URL"));
        source.setUsername(required(usernameKey));
        source.setPassword(required(passwordKey));
        return source;
    }
    public static String required(String name) {
        String value=System.getenv(name);
        if (value==null || value.isBlank()) throw new IllegalStateException("Missing environment setting: "+name);
        return value;
    }
}
