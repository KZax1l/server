/*
 * Tigase Jabber/XMPP Utils
 * Copyright (C) 2004-2012 "Artur Hefczyc" <artur.hefczyc@tigase.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. Look for COPYING file in the top folder.
 * If not, see http://www.gnu.org/licenses/.
 *
 * $Rev$
 * Last modified by $Author$
 * $Date$
 */
package tigase.util;

import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.logging.Level;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Describe class Algorithms here.
 *
 *
 * Created: Wed May  4 13:24:03 2005
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class Algorithms {

  /**
   * Creates a new <code>Algorithms</code> instance.
   *
   */
  private Algorithms() { }

  /**
   * This method encodes data using digest algorithm described in
   * <em>JEP-0078</em> documentation.
   * As a result you have <code>String</code> containing digest data which
   * can be compared with data sent by the user to authenticate him.
   *
   * @param id a <code>String</code> value of some ID value like session ID to
   * concatenate with secret word.
   * @param secret a <code>String</code> value of a secret word shared between
   * entites.
   * @param alg a <code>String</code> value of algorithm name to use for
   * generating diffest message.
   * @return a <code>String</code> value digest message as defined.
   * @exception NoSuchAlgorithmException if an error occurs during encoding
   * digest message.
   */
  public static final String hexDigest(final String id, final String secret,
    final String alg) throws NoSuchAlgorithmException {
    return bytesToHex(digest(id, secret, alg));
  }

	private final static String HASH_ALGO = "SHA-256";

	private final static String HMAC_ALGO = "HmacSHA256";

	private final static byte[] NULL_CHARS_ARRAY = new byte[] { 110, 117, 108, 108 };

	public static final byte[] digest(final String id, final String secret, final String alg) throws NoSuchAlgorithmException {
		MessageDigest md = MessageDigest.getInstance(alg);

		if (id == null)
			md.update(NULL_CHARS_ARRAY);
		else
			md.update(id.getBytes());

		if (secret == null)
			md.update(NULL_CHARS_ARRAY);
		else
			md.update(secret.getBytes());

		return md.digest();
	}

	public static final String bytesToHex(final byte[] buff) {
    StringBuilder res = new StringBuilder();
    for (byte b : buff) {
      char ch = Character.forDigit((b >> 4) & 0xF, 16);
      res.append(ch);
      ch = Character.forDigit(b & 0xF, 16);
      res.append(ch);
    } // end of for (b : digest)
    return res.toString();
	}

	private static String help() {
		return
			" -id id				id used to calculate digest\n"
			+ " -pass pass			password phrase for digest calculation\n"
			+ " -alg alg			algorith to use for calculating digest\n"
			;
	}

	/**
	 * Describe <code>main</code> method here.
	 *
	 * @param args a <code>String[]</code> value
	 */
	public static void $main(final String[] args) throws Exception {
		String id = null;
		String pass = null;
		String alg = "MD5";
		for (int i = 0; i < args.length; i++) {
			if (args[i].equals("-h")) {
				System.out.println(help());
				System.exit(0);
			} // end of if (args[i].equals("-id"))
			if (args[i].equals("-id")) {
				id = args[++i];
			} // end of if (args[i].equals("-id"))
			if (args[i].equals("-pass")) {
				pass = args[++i];
			} // end of if (args[i].equals("-id"))
			if (args[i].equals("-alg")) {
				alg = args[++i];
			} // end of if (args[i].equals("-id"))
		} // end of for (int i = 0; i < args.length; i++)
		if (id == null) {
			id = "";
		} // end of if (id == null)
		System.out.println(hexDigest(id, pass, alg));
	}

	
	/**
	 * Calculates dialback key as decribed in <a
	 * href='http://xmpp.org/extensions/xep-0185.html'>XEP-0185</a> version 1.0.
	 * 
	 * <p>
	 * Implemented algorithm (recomended in XEP-0185):
	 * 
	 * <pre>
	 * key = HMAC-SHA256
	 *       ( 
	 *         SHA256(Secret), 
	 *         { 
	 *           Receiving Server, ' ', 
	 *           Originating Server, ' ', 
	 *           Stream ID 
	 *         } 
	 *       )
	 * </pre>
	 * 
	 * </p>
	 * 
	 * @param originatingServer
	 *            the hostname of the Originating Server
	 * @param receivingServer
	 *            the hostname of the Receiving Server
	 * @param secret
	 *            a secret known by the Authoritative Server's network
	 * @param streamID
	 *            the Stream ID generated by the Receiving Server
	 * @return string contains HEX encoded dialback key.
	 */
	public static String generateDialbackKey(String originatingServer, String receivingServer, String secret, String streamID) {
		try {
			final Charset charSet = Charset.forName("US-ASCII");
			final Mac sha = Mac.getInstance(HMAC_ALGO);

			final SecretKeySpec secret_key = new SecretKeySpec(charSet.encode(sha256(secret)).array(),
					HMAC_ALGO);
			sha.init(secret_key);

			sha.update(receivingServer.getBytes());
			sha.update((byte) ' ');
			sha.update(originatingServer.getBytes());
			sha.update((byte) ' ');
			sha.update(streamID.getBytes());

			return bytesToHex(sha.doFinal());

		} catch (Exception e) {
			// log.log(Level.WARNING, "Can't generate dialback key", e);
			e.printStackTrace();
			return null;
		}

	}

	/**
	 * Calculates SHA-256 hash of given data.
	 * 
	 * @param data
	 *            data to hash
	 * @return string contains HEX encoded SHA-256 of data.
	 */
	public static String sha256(String data) {
		try {
			MessageDigest sha = MessageDigest.getInstance(HASH_ALGO);
			return bytesToHex(sha.digest(data.getBytes()));
		} catch (Exception e) {
			e.printStackTrace();
			// log.log(Level.WARNING, "Can't calculate Hash", e);
			return null;
		}
	}
} // Algorithms