package minerva.android.blockchainprovider.contract;

import io.reactivex.Flowable;
import io.reactivex.functions.Function;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.web3j.abi.EventEncoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.DynamicBytes;
import org.web3j.abi.datatypes.Event;
import org.web3j.abi.datatypes.Type;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.RemoteFunctionCall;
import org.web3j.protocol.core.methods.request.EthFilter;
import org.web3j.protocol.core.methods.response.BaseEventResponse;
import org.web3j.protocol.core.methods.response.Log;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
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
 * <p>Generated with web3j version 4.5.15.
 */
@SuppressWarnings("rawtypes")
public class ProxyFactory extends Contract {
    public static final String BINARY = "Bin file was not provided";

    public static final String FUNC_CREATEPROXY = "createProxy";

    public static final String FUNC_PROXYRUNTIMECODE = "proxyRuntimeCode";

    public static final String FUNC_PROXYCREATIONCODE = "proxyCreationCode";

    public static final String FUNC_CREATEPROXYWITHNONCE = "createProxyWithNonce";

    public static final String FUNC_CREATEPROXYWITHCALLBACK = "createProxyWithCallback";

    public static final String FUNC_CALCULATECREATEPROXYWITHNONCEADDRESS = "calculateCreateProxyWithNonceAddress";

    public static final Event PROXYCREATION_EVENT = new Event("ProxyCreation",
            Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {
            }));
    ;

    @Deprecated
    protected ProxyFactory(String contractAddress, Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    protected ProxyFactory(String contractAddress, Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, credentials, contractGasProvider);
    }

    @Deprecated
    protected ProxyFactory(String contractAddress, Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    protected ProxyFactory(String contractAddress, Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, transactionManager, contractGasProvider);
    }

    public List<ProxyCreationEventResponse> getProxyCreationEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = extractEventParametersWithLog(PROXYCREATION_EVENT, transactionReceipt);
        ArrayList<ProxyCreationEventResponse> responses = new ArrayList<ProxyCreationEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            ProxyCreationEventResponse typedResponse = new ProxyCreationEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.proxy = (String) eventValues.getNonIndexedValues().get(0).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public Flowable<ProxyCreationEventResponse> proxyCreationEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(new Function<Log, ProxyCreationEventResponse>() {
            @Override
            public ProxyCreationEventResponse apply(Log log) {
                Contract.EventValuesWithLog eventValues = extractEventParametersWithLog(PROXYCREATION_EVENT, log);
                ProxyCreationEventResponse typedResponse = new ProxyCreationEventResponse();
                typedResponse.log = log;
                typedResponse.proxy = (String) eventValues.getNonIndexedValues().get(0).getValue();
                return typedResponse;
            }
        });
    }

    public Flowable<ProxyCreationEventResponse> proxyCreationEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(PROXYCREATION_EVENT));
        return proxyCreationEventFlowable(filter);
    }

    public RemoteFunctionCall<TransactionReceipt> createProxy(String masterCopy, byte[] data) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(
                FUNC_CREATEPROXY,
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(160, masterCopy),
                        new org.web3j.abi.datatypes.DynamicBytes(data)),
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<byte[]> proxyRuntimeCode() {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(FUNC_PROXYRUNTIMECODE,
                Arrays.<Type>asList(),
                Arrays.<TypeReference<?>>asList(new TypeReference<DynamicBytes>() {
                }));
        return executeRemoteCallSingleValueReturn(function, byte[].class);
    }

    public RemoteFunctionCall<byte[]> proxyCreationCode() {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(FUNC_PROXYCREATIONCODE,
                Arrays.<Type>asList(),
                Arrays.<TypeReference<?>>asList(new TypeReference<DynamicBytes>() {
                }));
        return executeRemoteCallSingleValueReturn(function, byte[].class);
    }

    public RemoteFunctionCall<TransactionReceipt> createProxyWithNonce(String _mastercopy, byte[] initializer, BigInteger saltNonce) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(
                FUNC_CREATEPROXYWITHNONCE,
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(160, _mastercopy),
                        new org.web3j.abi.datatypes.DynamicBytes(initializer),
                        new org.web3j.abi.datatypes.generated.Uint256(saltNonce)),
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> createProxyWithCallback(String _mastercopy, byte[] initializer, BigInteger saltNonce, String callback) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(
                FUNC_CREATEPROXYWITHCALLBACK,
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(160, _mastercopy),
                        new org.web3j.abi.datatypes.DynamicBytes(initializer),
                        new org.web3j.abi.datatypes.generated.Uint256(saltNonce),
                        new org.web3j.abi.datatypes.Address(160, callback)),
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> calculateCreateProxyWithNonceAddress(String _mastercopy, byte[] initializer, BigInteger saltNonce) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(
                FUNC_CALCULATECREATEPROXYWITHNONCEADDRESS,
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(160, _mastercopy),
                        new org.web3j.abi.datatypes.DynamicBytes(initializer),
                        new org.web3j.abi.datatypes.generated.Uint256(saltNonce)),
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    @Deprecated
    public static ProxyFactory load(String contractAddress, Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        return new ProxyFactory(contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    @Deprecated
    public static ProxyFactory load(String contractAddress, Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        return new ProxyFactory(contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    public static ProxyFactory load(String contractAddress, Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        return new ProxyFactory(contractAddress, web3j, credentials, contractGasProvider);
    }

    public static ProxyFactory load(String contractAddress, Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        return new ProxyFactory(contractAddress, web3j, transactionManager, contractGasProvider);
    }

    public static class ProxyCreationEventResponse extends BaseEventResponse {
        public String proxy;
    }
}
