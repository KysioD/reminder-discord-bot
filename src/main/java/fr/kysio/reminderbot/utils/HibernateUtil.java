package fr.kysio.reminderbot.utils;

import fr.kysio.reminderbot.data.ExecutionDay;
import fr.kysio.reminderbot.data.Reminder;
import fr.kysio.reminderbot.data.ReminderHistory;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

import java.util.Properties;

public class HibernateUtil {

    public static SessionFactory sessionFactory;

    private static Properties setupHibernateConfig() {
        Properties prop = new Properties();

        prop.setProperty("hibernate.connection.url", System.getenv("JDBC_DATABASE_URL"));
        prop.setProperty("hibernate.connection.username", System.getenv("JDBC_DATABASE_USERNAME"));
        prop.setProperty("hibernate.connection.password", System.getenv("JDBC_DATABASE_PASSWORD"));
        prop.setProperty("hibernate.connection.driver_class", "org.postgresql.Driver");
        prop.setProperty("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
        prop.setProperty("hibernate.hbm2ddl.auto", "update");
        prop.setProperty("hibernate.show_sql", "true");

        return prop;
    }

    public static void createSessionFactory() {
        Configuration configuration = new Configuration();
        configuration.setProperties(setupHibernateConfig());
        configuration.addAnnotatedClass(Reminder.class);
        configuration.addAnnotatedClass(ReminderHistory.class);
        configuration.addAnnotatedClass(ExecutionDay.class);
        HibernateUtil.sessionFactory = configuration.buildSessionFactory();
    }

}
