/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package aceleradevcsharp;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.net.ssl.HttpsURLConnection;
import org.apache.commons.io.IOUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.json.simple.parser.ParseException;

/**
 *
 * @author tiago
 */
public class AceleraDevCsharp {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        // TODO code application logic here
        String get = "https://api.codenation.dev/v1/challenge/dev-ps/generate-data?token=";
        String post = "https://api.codenation.dev/v1/challenge/dev-ps/submit-solution?token=";
        String token = "a751aa3db63d2f5e4fb1350f3e1d3472bda910d8";

        try {
            URL urlGet = new URL(get.concat(token));
            HttpsURLConnection con = (HttpsURLConnection) urlGet.openConnection();

            con.setRequestMethod("GET");
            con.setRequestProperty("Content-Type", "application/json");
            con.connect();

            if (con.getResponseCode() != 200) {
                throw new RuntimeException("HTTP error code: " + con.getResponseMessage());
            }

            InputStream in = con.getInputStream();
            String encoding = con.getContentEncoding();
            encoding = encoding == null ? "UTF-8" : encoding;
            String rs = IOUtils.toString(in, encoding);

            con.disconnect();

            JSONObject jso = (JSONObject) new JSONParser().parse(rs);

            FileWriter file = new FileWriter("answer.json");
            file.write(jso.toJSONString());
            file.flush();

            String nr_casas = jso.get("numero_casas").toString();
            String cifrado = jso.get("cifrado").toString();

            Character[] alfabeto = "abcdefghijklmnopqrstuvwxyz".chars().mapToObj(c -> (char) c).toArray(Character[]::new);
            Character[] cif = cifrado.chars().mapToObj(c -> (char) c).toArray(Character[]::new);
            String decifrado = "";

            for (int i = 0; i < cif.length; i++) {
                if (!Character.isLetter(cif[i])) {
                    decifrado += cif[i];
                    continue;
                }
                for (int j = 0; j < alfabeto.length; j++) {
                    if (Objects.equals(cif[i], alfabeto[j])) {
                        int z = j - Integer.parseInt(nr_casas);
                        if (z < 0) {
                            z = 26 + z;
                        }
                        decifrado += alfabeto[z];
                    }
                }
            }

            FileReader reader = new FileReader("answer.json");
            jso = (JSONObject) new JSONParser().parse(reader);
            jso.replace("decifrado", decifrado);
            jso.replace("resumo_criptografico", sha1(decifrado));

            file = new FileWriter("answer.json");
            file.write(jso.toJSONString());
            file.flush();

            CloseableHttpClient httpClient = HttpClients.createDefault();
            HttpPost uploadFile = new HttpPost(post.concat(token));
            MultipartEntityBuilder builder = MultipartEntityBuilder.create();
            builder.addTextBody("field1", "yes", ContentType.TEXT_PLAIN);

            // This attaches the file to the POST:
            File f = new File("answer.json");
            builder.addBinaryBody(
                    "answer",
                    new FileInputStream(f),
                    ContentType.APPLICATION_OCTET_STREAM,
                    f.getName()
            );

            HttpEntity multipart = builder.build();
            uploadFile.setEntity(multipart);
            CloseableHttpResponse response = httpClient.execute(uploadFile);
            HttpEntity responseEntity = response.getEntity();

        } catch (IOException | RuntimeException | NoSuchAlgorithmException | ParseException ex) {
            Logger.getLogger(AceleraDevCsharp.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    private static String sha1(String t) throws NoSuchAlgorithmException, IOException {
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        ByteArrayInputStream fis = new ByteArrayInputStream(t.getBytes());

        byte[] dataBytes = new byte[1024];

        int nread = 0;
        while ((nread = fis.read(dataBytes)) != -1) {
            md.update(dataBytes, 0, nread);
        };
        byte[] mdbytes = md.digest();

        //convert the byte to hex format method 1
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < mdbytes.length; i++) {
            sb.append(Integer.toString((mdbytes[i] & 0xff) + 0x100, 16).substring(1));
        }

        return sb.toString();
    }

}
