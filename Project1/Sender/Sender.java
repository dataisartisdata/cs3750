import java.io.*;
import java.math.BigInteger;
import java.security.DigestInputStream;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.RSAPrivateKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.Scanner;
import javax.crypto.Cipher;
import java.security.MessageDigest;
import java.util.Arrays;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.crypto.spec.IvParameterSpec;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class Sender {

	public Sender() {
	}
	
	public static void main(String[] args) throws Exception{
		String KXY = readKXYFromFile("symmetric.key");
	    PrivateKey KXPrivate = readPrivKeyFromFile("XPrivate.key");
	    Scanner in = new Scanner(System.in);
	    System.out.print("Input the name of the message file: ");
	    String msg = in.next();
	    byte[] msgAsByte = toByteArr(msg);
	    in.close();
	    byte[] hash = md(msg).getBytes();
	    System.out.println("digit digest (hash value):");
	    toHexa(hash);
    	saveToFile("message.dd", hash);
    	byte[] cipherText = encryptRSA(KXPrivate,hash);
    	System.out.println("CipherText:");
	    toHexa(cipherText);
    	System.out.println("");
    	saveToFile("message.dd-msg",cipherText);
    	append("message.dd-msg",msgAsByte);
      byte[] IV = randomIV();
      byte[] digSigAndMsg = toByteArr("message.dd-msg");
      byte[] aesCipher = encryptAES(KXY,IV,digSigAndMsg);
      saveToFile("message.aescipher",IV);
      append("message.aescipher",aesCipher);
	}
   
   public static byte[] encryptRSA(PrivateKey KXPrivate, byte[] hash) throws Exception {
      Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
      SecureRandom random = new SecureRandom();
    	cipher.init(Cipher.ENCRYPT_MODE, KXPrivate, random);
    	return cipher.doFinal(hash);
   }
 
	public static byte[] randomIV(){
      SecureRandom random = new SecureRandom();
      byte[] bytes = new byte[16];
      random.nextBytes(bytes);
      return bytes;
   }
   
   public static byte[] encryptAES(String symmetricKey, byte[] IV, byte[] digSigAndMsg) throws Exception {
      Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding", "SunJCE");
      SecretKeySpec key = new SecretKeySpec(symmetricKey.getBytes("UTF-8"), "AES");
      cipher.init(Cipher.ENCRYPT_MODE, key,new IvParameterSpec(IV));
      return cipher.doFinal(digSigAndMsg);
   }
   
	public static void toHexa(byte [] in) {
		for (int k=0, j=0; k<in.length; k++, j++) {
			System.out.format("%2X ", new Byte(in[k])) ;
		    if (j >= 15) {
		    	System.out.println("");
		        j=-1;
		    }
		}
	}
	
	public static byte[] toByteArr(String file) throws Exception {
		FileInputStream fileInputStream = null;
	   byte[] ba = null;
	   try {
	      File f = new File(file);
	      ba = new byte[(int) f.length()];
	      //read file into bytes[]
	      fileInputStream = new FileInputStream(f);
	      fileInputStream.read(ba);
	   } catch (IOException e) {
	    	e.printStackTrace();
	   } finally {
	    	if (fileInputStream != null) {
	      try {
	         fileInputStream.close();
	      } catch (IOException e) {
	        	e.printStackTrace();
	      }
	      }
	   }
	   return ba;
	}
	
	public static void append(String fileName, byte[] data) throws Exception {
		System.out.println("append to " + fileName + "\n");
		OutputStream os = null;
		try {
			// below true flag tells OutputStream to append
			os = new FileOutputStream(new File(fileName), true);
			os.write(data, 0, data.length);
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				os.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	public static void saveToFile(String fileName, byte [] arr) throws Exception {
		System.out.println("Write to " + fileName + "\n");
		FileOutputStream fos = new FileOutputStream(fileName);
		try {
			fos.write(arr);
		}
		finally {
			fos.close();
		}
	}
   
	public static String md(String f) throws Exception {
	   BufferedInputStream file = new BufferedInputStream(new FileInputStream(f));
	   MessageDigest md = MessageDigest.getInstance("SHA-256");
	   DigestInputStream in = new DigestInputStream(file, md);
      int BUFFER_SIZE = 32 * 1024;
	   int i;
	   byte[] buffer = new byte[BUFFER_SIZE];
	   do {
	      i = in.read(buffer, 0, BUFFER_SIZE);
	   } while (i == BUFFER_SIZE);
	   md = in.getMessageDigest();
	   in.close();
	   byte[] hash = md.digest();
	   System.out.println("");    
	   return new String(hash);
	}
	
	//needs to be changed from string to and OBJECT?
	public static String readKXYFromFile(String keyFileName) 
		      throws IOException {
		InputStream in = 
				Sender.class.getResourceAsStream(keyFileName);
		ObjectInputStream oin =
				new ObjectInputStream(new BufferedInputStream(in));
		try {
			String m = (String) oin.readObject();
			System.out.println("Read from " + keyFileName + ": msg= " + 
					m.toString()  + "\n");
		    String key = m.toString();
		    return key;
		} catch (Exception e) {
			throw new RuntimeException("Spurious serialisation error", e);
		} finally {
		    oin.close();
		}
	}

	//read key parameters from a file and generate the private key 
	public static PrivateKey readPrivKeyFromFile(String keyFileName) 
			throws IOException {
		InputStream in = 
				Sender.class.getResourceAsStream(keyFileName);
		ObjectInputStream oin =
		   		new ObjectInputStream(new BufferedInputStream(in));
		try {
			BigInteger m = (BigInteger) oin.readObject();
		    BigInteger e = (BigInteger) oin.readObject();
		    System.out.println("Read from " + keyFileName + ": modulus = " + 
		    		m.toString() + ", exponent = " + e.toString() + "\n");
		    RSAPrivateKeySpec keySpec = new RSAPrivateKeySpec(m, e);
		    KeyFactory factory = KeyFactory.getInstance("RSA");
		    PrivateKey key = factory.generatePrivate(keySpec);
		    return key;
		} catch (Exception e) {
			throw new RuntimeException("Spurious serialisation error", e);
		} finally {
		    oin.close();
		}
	}
}
