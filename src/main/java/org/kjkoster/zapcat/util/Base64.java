package org.kjkoster.zapcat.util;

import java.nio.charset.Charset;

/* This file is part of Zapcat.
*
* Zapcat is free software: you can redistribute it and/or modify it under the
* terms of the GNU General Public License as published by the Free Software
* Foundation, either version 3 of the License, or (at your option) any later
* version.
* 
* Zapcat is distributed in the hope that it will be useful, but WITHOUT ANY
* WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
* FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
* details.
* 
* You should have received a copy of the GNU General Public License along with
* Zapcat. If not, see <http://www.gnu.org/licenses/>.
*/

/**
 * A Base64 utility class that performs static Base64 string encoding.
 * 
 * @author Brett Cave &lt;brettcave@gmail.com&gt;
 *
 */
public class Base64 {
 
    public static String base64code = "ABCDEFGHIJKLMNOPQRSTUVWXYZ" +
        "abcdefghijklmnopqrstuvwxyz" + 
        "0123456789" + "+/";
 
    public static int splitLinesAt = 76;
 
    public static byte[] zeroPad(int length, byte[] bytes) {
        byte[] padded = new byte[length]; // initialized to zero by JVM
        System.arraycopy(bytes, 0, padded, 0, bytes.length);
        return padded;
    }

    
    /**
     * Encodes a string as per the Base64 RFC (RFC3548 / RFC4648), using the default character set.
     * 
     * @param String string - the string to encode.
     * 
     * @return String - the Base64 encoded string.
     */
    public static String encode(String string) {
    	return encode(string,Charset.defaultCharset().name());
    }
 
    /**
     * Encodes a string as per the Base64 RFC (RFC3548 / RFC4648), using a specific character set.
     * If the specified character set cannot be used to encode the string, the system default
     * character set will be used.
     * 
     * @param String string - the string to encode.
     * 
     * @param String charset - the character set to use to encode the string (e.g. UTF-8)
     * 
     * @return String - the Base64 encoded string. 
     */
    public static String encode(String string, String charset) {
 
        String encoded = "";
        byte[] stringArray;
        try {
            stringArray = string.getBytes(charset);
        } catch (Exception ignored) {
            stringArray = string.getBytes();  // use locale default rather than croak
        }
        
        // determine how many padding bytes to add to the output
        int paddingCount = (3 - (stringArray.length % 3)) % 3;
        // add any necessary padding to the input
        stringArray = zeroPad(stringArray.length + paddingCount, stringArray);
        // process 3 bytes at a time, churning out 4 output bytes
        // worry about CRLF insertions later
        for (int i = 0; i < stringArray.length; i += 3) {
            int j = ((stringArray[i] & 0xff) << 16) +
                ((stringArray[i + 1] & 0xff) << 8) + 
                (stringArray[i + 2] & 0xff);
            encoded = encoded + base64code.charAt((j >> 18) & 0x3f) +
                base64code.charAt((j >> 12) & 0x3f) +
                base64code.charAt((j >> 6) & 0x3f) +
                base64code.charAt(j & 0x3f);
        }
        // replace encoded padding nulls with "="
        return splitLines(encoded.substring(0, encoded.length() -
            paddingCount) + "==".substring(0, paddingCount));
    }
    
    public static String splitLines(String string) {
		if(string.length()<splitLinesAt)
			return string;
		else{
			String lines = "";
			for (int i = 0; i < string.length(); i += splitLinesAt) {
				lines += string.substring(i, Math.min(string.length(), i + splitLinesAt));
				lines += "\r\n";
			}
        return lines;
		}
    }
 
}
