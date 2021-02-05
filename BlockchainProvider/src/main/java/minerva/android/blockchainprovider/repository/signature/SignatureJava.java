package minerva.android.blockchainprovider.repository.signature;

import org.web3j.crypto.Sign;
import org.web3j.utils.Numeric;

import java.util.Arrays;

final public class SignatureJava {

    public static final String MESSAGE_PREFIX = "\u0019Ethereum Signed Message:\n";

    static byte[] getEthereumMessage(byte[] message) {
        byte[] prefix = MESSAGE_PREFIX.concat(String.valueOf(message.length)).getBytes();
        byte[] result = new byte[prefix.length + message.length];
        System.arraycopy(prefix, 0, result, 0, prefix.length);
        System.arraycopy(message, 0, result, prefix.length, message.length);
        return result;
    }

    static byte[] patchSignatureVComponent(byte[] signature) {
        if (signature != null && signature.length == 65 && signature[64] < 27) {
            signature[64] = (byte) (signature[64] + (byte) 0x1b);
        }

        return signature;
    }

    public static byte[] bytesFromSignature(Sign.SignatureData signature) {
        byte[] sigBytes = new byte[65];
        Arrays.fill(sigBytes, (byte) 0);

        try {
            System.arraycopy(signature.getR(), 0, sigBytes, 0, 32);
            System.arraycopy(signature.getS(), 0, sigBytes, 32, 32);
            System.arraycopy(signature.getV(), 0, sigBytes, 64, 1);
        } catch (IndexOutOfBoundsException e) {
            e.printStackTrace();
        }

        return sigBytes;
    }

    public static byte[] hexStringToByteArray(String input) {
        String cleanInput = cleanHexPrefix(input);

        int len = cleanInput.length();

        if (len == 0) {
            return new byte[] {};
        }

        byte[] data;
        int startIdx;
        if (len % 2 != 0) {
            data = new byte[(len / 2) + 1];
            data[0] = (byte) Character.digit(cleanInput.charAt(0), 16);
            startIdx = 1;
        } else {
            data = new byte[len / 2];
            startIdx = 0;
        }

        for (int i = startIdx; i < len; i += 2) {
            data[(i + 1) / 2] = (byte) ((Character.digit(cleanInput.charAt(i), 16) << 4)
                    + Character.digit(cleanInput.charAt(i + 1), 16));
        }
        return data;
    }

    public static String cleanHexPrefix(String input) {
        if (containsHexPrefix(input)) {
            return input.substring(2);
        } else {
            return input;
        }
    }

    public static boolean containsHexPrefix(String input) {
        return input.length() > 1 && input.charAt(0) == '0' && input.charAt(1) == 'x';
    }

}
