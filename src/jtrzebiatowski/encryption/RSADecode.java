package jtrzebiatowski.encryption;

import javax.crypto.Cipher;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.RSAPrivateCrtKeySpec;

public class RSADecode {

    public static byte[] decryptRSA(ByteBuffer encryptedBuffer, PrivateKey privateKey) throws Exception {
        byte[] encryptedData = new byte[encryptedBuffer.remaining()];
        encryptedBuffer.get(encryptedData);

        Cipher cipher = Cipher.getInstance("RSA/ECB/NOPADDING");
        cipher.init(Cipher.DECRYPT_MODE, privateKey);

        return cipher.doFinal(encryptedData);
    }

    public static PrivateKey getPrivateKeyFromComponents(BigInteger p, BigInteger q, BigInteger dp, BigInteger dq, BigInteger e) throws Exception {
        BigInteger n = p.multiply(q);
        BigInteger phi = p.subtract(BigInteger.ONE).multiply(q.subtract(BigInteger.ONE));
        BigInteger d = e.modInverse(phi);
        BigInteger qInv = q.modInverse(p);

        RSAPrivateCrtKeySpec keySpec = new RSAPrivateCrtKeySpec(n, e, d, p, q, dp, dq, qInv);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePrivate(keySpec);
    }

    public static void decode(ByteBuffer byteBuffer) {
        byteBuffer.mark();
        try {
            // Values for OTServer
            BigInteger p = new BigInteger("14299623962416399520070177382898895550795403345466153217470516082934737582776038882967213386204600674145392845853859217990626450972452084065728686565928113");
            BigInteger q = new BigInteger("7630979195970404721891201847792002125535401292779123937207447574596692788513647179235335529307251350570728407373705564708871762033017096809910315212884101");
            BigInteger dp = new BigInteger("11141736698610418925078406669215087697114858422461871124661098818361832856659225315773346115219673296375487744032858798960485665997181641221483584094519937");
            BigInteger dq = new BigInteger("4886309137722172729208909250386672706991365415741885286554321031904881408516947737562153523770981322408725111241551398797744838697461929408240938369297973");
            BigInteger e = BigInteger.valueOf(65537); // Common public exponent

            PrivateKey privateKey = getPrivateKeyFromComponents(p, q, dp, dq, e);

            // Simulate encrypted  in ByteBuffer

            // Decrypt
            byte[] decryptedData = decryptRSA(byteBuffer, privateKey);

            byteBuffer.reset();
            byteBuffer.put(decryptedData);


        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
