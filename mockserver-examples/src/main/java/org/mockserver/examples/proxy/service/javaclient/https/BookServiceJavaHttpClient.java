package org.mockserver.examples.proxy.service.javaclient.https;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.mockserver.examples.proxy.model.Book;
import org.mockserver.examples.proxy.service.BookService;
import org.mockserver.logging.MockServerLogger;
import org.mockserver.socket.tls.KeyStoreFactory;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;

import static org.mockserver.examples.proxy.json.ObjectMapperFactory.createObjectMapper;

/**
 * @author jamesdbloom
 */
@Component
public class BookServiceJavaHttpClient implements BookService {

    @Resource
    private Environment environment;
    private Integer port;
    private String host;
    private ObjectMapper objectMapper;

    @PostConstruct
    private void initialise() {
        port = Integer.parseInt(System.getProperty("bookService.port"));
        host = environment.getProperty("bookService.host", "localhost");
        objectMapper = createObjectMapper();
    }

    private HttpURLConnection sendRequestViaProxy(URL url) throws IOException {
        Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(System.getProperty("http.proxyHost"), Integer.parseInt(System.getProperty("http.proxyPort"))));
        SSLContext sslContext = new KeyStoreFactory(new MockServerLogger(this.getClass())).sslContext();
        HttpsURLConnection httpURLConnection = (HttpsURLConnection) url.openConnection(proxy);
        httpURLConnection.setSSLSocketFactory(sslContext.getSocketFactory());
        return httpURLConnection;
    }

    @Override
    public Book[] getAllBooks() {
        try {
            logger.info("Sending request to https://" + host + ":" + port + "/get_books");
            HttpURLConnection connection = sendRequestViaProxy(new URL("https://" + host + ":" + port + "/get_books"));
            connection.setRequestMethod("GET");
            return objectMapper.readValue(connection.getInputStream(), Book[].class);
        } catch (Exception e) {
            logger.info("Exception sending request to https://" + host + ":" + port + "/get_books", e);
            throw new RuntimeException("Exception making request to retrieve all books", e);
        }
    }

    @Override
    public Book getBook(String id) {
        try {
            logger.info("Sending request to https://" + host + ":" + port + "/get_book?id=" + id);
            HttpURLConnection connection = sendRequestViaProxy(new URL("https://" + host + ":" + port + "/get_book?id=" + id));
            connection.setRequestMethod("GET");
            return objectMapper.readValue(connection.getInputStream(), Book.class);
        } catch (Exception e) {
            logger.info("Exception sending request to https://" + host + ":" + port + "/get_books", e);
            throw new RuntimeException("Exception making request to retrieve a book with id [" + id + "]", e);
        }
    }
}
