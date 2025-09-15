package io.github.davidvsaraiva.merlin.monitor;

import java.util.List;
import java.util.Properties;

import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import static ch.qos.logback.core.util.OptionHelper.getEnv;
import static io.github.davidvsaraiva.merlin.monitor.Config.getEnvOrDefault;

public class EmailNotifier {

    private static final String DELIMITER = ",";

    private final Session session;
    private final String from;
    private final List<String> destinations;

    public EmailNotifier(String host, int port, boolean starttls, String username, String password, String from,
            List<String> destinations) {
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", Boolean.toString(starttls));
        props.put("mail.smtp.host", host);
        props.put("mail.smtp.port", Integer.toString(port));

        this.session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
            }
        });
        this.from = from;
        this.destinations = destinations;
    }

    public static EmailNotifier fromEnv() {
        String host = System.getenv("SMTP_HOST");
        int port = Integer.parseInt(getEnvOrDefault("SMTP_PORT", "587"));
        boolean starttls = Boolean.parseBoolean(getEnvOrDefault("SMTP_STARTTLS", "true"));
        String user = getEnv("SMTP_USERNAME");
        String password = getEnv("SMTP_PASSWORD");
        String from = getEnv("SMTP_FROM");
        List<String> to = List.of(getEnv("SMTP_TO").split(DELIMITER));
        return new EmailNotifier(host, port, starttls, user, password, from, to);
    }

    public void send(String subject, String body) throws MessagingException {
        for(String to: destinations) {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(from));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
            message.setSubject(subject);
            message.setText(body);
            Transport.send(message);
        }
    }
}