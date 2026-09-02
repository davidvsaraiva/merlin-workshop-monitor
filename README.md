# Workshop Monitor

A Java 17 application that monitors when Merlin's has new workshops and notifies you by **email** when a new workshop is added.

The app stores "seen" workshop URLs in a JSON file in the filesystem to avoid sending duplicate notifications.

The app reads Leroy Merlin's public workshops sitemap (`sitemap-idee-projet1.xml`) to detect newly-added workshop pages — no browser automation involved.

The app can run in single or scheduled mode. The initial trigger was to run this in scheduled mode in a lower power consumption device like a Raspberry Pi.

---

## Build

```bash
mvn clean package
```

## Run

```bash
# default -> scheduled to run every hour
java -jar merlin-workshop-monitor-X.X.X-all.jar
# or
java -jar merlin-workshop-monitor-X.X.X-all.jar --once
# or
java -jar merlin-workshop-monitor-X.X.X-all.jar --interval-minutes 120
```
---

## Environment variables:
      SMTP_HOST                SMTP server hostname (e.g. smtp.gmail.com)
      SMTP_PORT                SMTP port (default 587)
      SMTP_STARTTLS            true|false (default true)
      SMTP_USERNAME            SMTP username (e.g. your@gmail.com)
      SMTP_PASSWORD            SMTP password or app password
      SMTP_FROM                From email address
      SMTP_TO                  To email addresses (delimited by ",")
      LOG_LEVEL                Log level for the application logs

## Politeness

- Keep reasonable intervals to avoid stressing the site, even though the sitemap is a
  lightweight, publicly-intended endpoint.

---

## License

MIT (adapt as you wish).

---
