package fr.kysio.reminderbot.utils;

import fr.kysio.reminderbot.data.ExecutionDay;
import fr.kysio.reminderbot.data.Reminder;
import fr.kysio.reminderbot.data.ReminderHistory;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Properties;

public class HibernateUtil {

    public static SessionFactory sessionFactory;

    private static Properties setupHibernateConfig() throws URISyntaxException {
        Properties prop = new Properties();

        final URI dbUrl = new URI(System.getenv("DATABASE_URL"));
        final String username = dbUrl.getUserInfo().split(":")[0];
        final String password = dbUrl.getUserInfo().split(":")[1];


        prop.setProperty("hibernate.connection.url", "jdbc:postgresql://"+dbUrl.getHost()+":"+dbUrl.getPort()+dbUrl.getPath()+"?sslmode=require");
        prop.setProperty("hibernate.connection.username", username);
        prop.setProperty("hibernate.connection.password", password);
        prop.setProperty("hibernate.connection.driver_class", "org.postgresql.Driver");
        prop.setProperty("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
        prop.setProperty("hibernate.hbm2ddl.auto", "update");
        prop.setProperty("hibernate.show_sql", "true");

        return prop;
    }

    public static void createSessionFactory() throws URISyntaxException {
        Configuration configuration = new Configuration();
        configuration.setProperties(setupHibernateConfig());
        configuration.addAnnotatedClass(Reminder.class);
        configuration.addAnnotatedClass(ReminderHistory.class);
        configuration.addAnnotatedClass(ExecutionDay.class);
        HibernateUtil.sessionFactory = configuration.buildSessionFactory();
    }

}
