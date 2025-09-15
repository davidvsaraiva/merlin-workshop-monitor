# Workshop Monitor

A Java 17 application that monitors when Merlin's has new workshops and notifies you by **email** when:

- New **Loulé** or **Albufeira** workshops appear.

The app stores "seen" items in JSON file in the filesystem to avoid sending duplicate notifications.

The app is designed for a specific Qualtrics form structure and it extracts information according to it.

The app can run in single or scheduled mode. The initial trigger was to run this in scheduled mode in a lower power consumption device like a Raspberry Pi.

---

## Build

```bash
mvn clean package
```

## Run

```bash
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
      FORM_TO_MONITOR_URL      Form to monitor URL
      LOG_LEVEL                Log level for the application logs
      IS_CHROMIUM              true|false (default false)
      CHROMIUM_BROWSER_PATH    path to chromium browser
      CHROMIUM_DRIVER_PATH     path to chromium driver

## Politeness

- Keep long intervals to avoid stressing the site.

---

## License

MIT (adapt as you wish).

---
