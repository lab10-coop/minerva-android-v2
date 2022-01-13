package minerva.android.blockchainprovider.smartContracts;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.HashMap;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.Utf8String;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.RemoteCall;
import org.web3j.protocol.core.RemoteFunctionCall;
import org.web3j.tx.Contract;
import org.web3j.tx.TransactionManager;
import org.web3j.tx.gas.ContractGasProvider;

/**
 * <p>Auto generated code.
 * <p><strong>Do not modify!</strong>
 * <p>Please use the <a href="https://docs.web3j.io/command_line.html">web3j command line tools</a>,
 * or the org.web3j.codegen.SolidityFunctionWrapperGenerator in the 
 * <a href="https://github.com/web3j/web3j/tree/master/codegen">codegen module</a> to update.
 *
 * <p>Generated with web3j version 1.4.1.
 */
@SuppressWarnings("rawtypes")
public class ERC1155Metadata_URI extends Contract {
    public static final String BINARY = "0x";

    public static final String FUNC_URI = "uri";

    protected static final HashMap<String, String> _addresses;

    static {
        _addresses = new HashMap<String, String>();
    }

    @Deprecated
    protected ERC1155Metadata_URI(String contractAddress, Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    protected ERC1155Metadata_URI(String contractAddress, Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, credentials, contractGasProvider);
    }

    @Deprecated
    protected ERC1155Metadata_URI(String contractAddress, Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    protected ERC1155Metadata_URI(String contractAddress, Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, transactionManager, contractGasProvider);
    }

    public RemoteFunctionCall<String> uri(BigInteger _id) {
        final Function function = new Function(FUNC_URI, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(_id)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Utf8String>() {}));
        return executeRemoteCallSingleValueReturn(function, String.class);
    }

    @Deprecated
    public static ERC1155Metadata_URI load(String contractAddress, Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        return new ERC1155Metadata_URI(contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    @Deprecated
    public static ERC1155Metadata_URI load(String contractAddress, Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        return new ERC1155Metadata_URI(contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    public static ERC1155Metadata_URI load(String contractAddress, Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        return new ERC1155Metadata_URI(contractAddress, web3j, credentials, contractGasProvider);
    }

    public static ERC1155Metadata_URI load(String contractAddress, Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        return new ERC1155Metadata_URI(contractAddress, web3j, transactionManager, contractGasProvider);
    }

    public static RemoteCall<ERC1155Metadata_URI> deploy(Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        return deployRemoteCall(ERC1155Metadata_URI.class, web3j, credentials, contractGasProvider, BINARY, "");
    }

    @Deprecated
    public static RemoteCall<ERC1155Metadata_URI> deploy(Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        return deployRemoteCall(ERC1155Metadata_URI.class, web3j, credentials, gasPrice, gasLimit, BINARY, "");
    }

    public static RemoteCall<ERC1155Metadata_URI> deploy(Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        return deployRemoteCall(ERC1155Metadata_URI.class, web3j, transactionManager, contractGasProvider, BINARY, "");
    }

    @Deprecated
    public static RemoteCall<ERC1155Metadata_URI> deploy(Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        return deployRemoteCall(ERC1155Metadata_URI.class, web3j, transactionManager, gasPrice, gasLimit, BINARY, "");
    }

    protected String getStaticDeployedAddress(String networkId) {
        return _addresses.get(networkId);
    }

    public static String getPreviouslyDeployedAddress(String networkId) {
        return _addresses.get(networkId);
    }
}
