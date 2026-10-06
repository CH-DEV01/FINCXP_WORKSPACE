package com.davivienda.factoraje.infrastructure.mail;

import java.net.URI;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Component;

import com.davivienda.factoraje.infrastructure.exception.EmailDeliveryException;
import com.mailjet.client.ClientOptions;
import com.mailjet.client.MailjetClient;
import com.mailjet.client.MailjetRequest;
import com.mailjet.client.errors.MailjetException;
import com.mailjet.client.errors.MailjetSocketTimeoutException;
import com.mailjet.client.resource.Emailv31;

@Component
public class MailjetTransport implements MailTransport {

    @Override
    public void deliver(String apiKey, String apiSecret, String apiUrl, String fromEmail, String fromName,
            OutboundMail mail) {
        JSONObject message = new JSONObject()
                .put(Emailv31.Message.FROM, new JSONObject()
                        .put("Email", fromEmail)
                        .put("Name", fromName))
                .put(Emailv31.Message.TO, recipients(mail.to()))
                .put(Emailv31.Message.SUBJECT, mail.subject())
                .put(Emailv31.Message.TEXTPART, mail.text())
                .put(Emailv31.Message.HTMLPART, mail.html());
        if (!mail.cc().isEmpty()) {
            message.put(Emailv31.Message.CC, recipients(mail.cc()));
        }

        MailjetRequest request = new MailjetRequest(Emailv31.resource)
                .property(Emailv31.MESSAGES, new JSONArray().put(message));
        MailjetClient client = new MailjetClient(apiKey, apiSecret, clientOptions(apiUrl));
        try {
            int status = client.post(request).getStatus();
            if (status != 200) {
                throw new EmailDeliveryException("Mailjet respondió con estado " + status + ".");
            }
        } catch (MailjetException | MailjetSocketTimeoutException e) {
            throw new EmailDeliveryException("No se pudo enviar el correo.", e);
        }
    }

    /**
     * El cliente arma {@code baseUrl/version/send}. La URL guardada es el endpoint completo,
     * por ejemplo {@code https://api.mailjet.com/v3/send}.
     */
    static ClientOptions clientOptions(String apiUrl) {
        URI uri;
        try {
            uri = new URI(apiUrl);
        } catch (Exception e) {
            throw new EmailDeliveryException("MAILJET_API_URL no es una URL válida.");
        }
        String path = uri.getRawPath();
        if (uri.getHost() == null || path == null || !path.endsWith("/send")) {
            throw new EmailDeliveryException("MAILJET_API_URL debe terminar en /send.");
        }
        String prefix = path.substring(0, path.length() - "/send".length());
        int slash = prefix.lastIndexOf('/');
        if (slash < 0 || slash == prefix.length() - 1) {
            throw new EmailDeliveryException("MAILJET_API_URL debe incluir la versión, por ejemplo /v3/send.");
        }
        String version = prefix.substring(slash + 1);
        String basePath = prefix.substring(0, slash);
        String baseUrl = uri.getScheme() + "://" + uri.getHost()
                + (uri.getPort() != -1 ? ":" + uri.getPort() : "")
                + basePath;
        return new ClientOptions(version, baseUrl);
    }

    private static JSONArray recipients(List<MailRecipient> recipients) {
        JSONArray array = new JSONArray();
        for (MailRecipient recipient : recipients) {
            JSONObject person = new JSONObject().put("Email", recipient.email());
            if (recipient.name() != null && !recipient.name().isBlank()) {
                person.put("Name", recipient.name());
            }
            array.put(person);
        }
        return array;
    }
}
