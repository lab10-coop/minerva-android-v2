package minerva.android.blockchainprovider.smartContracts;

import org.web3j.abi.EventEncoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.DynamicBytes;
import org.web3j.abi.datatypes.Event;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.generated.Bytes32;
import org.web3j.abi.datatypes.generated.Int256;
import org.web3j.abi.datatypes.generated.Int96;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.RemoteFunctionCall;
import org.web3j.protocol.core.methods.request.EthFilter;
import org.web3j.protocol.core.methods.response.BaseEventResponse;
import org.web3j.protocol.core.methods.response.Log;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.tuples.generated.Tuple3;
import org.web3j.tuples.generated.Tuple4;
import org.web3j.tx.Contract;
import org.web3j.tx.TransactionManager;
import org.web3j.tx.gas.ContractGasProvider;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;

import io.reactivex.Flowable;
import io.reactivex.functions.Function;

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
public class ConstantFlowAgreementV1 extends Contract {
    public static final String BINARY = "Bin file was not provided";

    public static final String FUNC_AGREEMENTTYPE = "agreementType";

    public static final String FUNC_GETCODEADDRESS = "getCodeAddress";

    public static final String FUNC_INITIALIZE = "initialize";

    public static final String FUNC_PROXIABLEUUID = "proxiableUUID";

    public static final String FUNC_UPDATECODE = "updateCode";

    public static final String FUNC_REALTIMEBALANCEOF = "realtimeBalanceOf";

    public static final String FUNC_GETMAXIMUMFLOWRATEFROMDEPOSIT = "getMaximumFlowRateFromDeposit";

    public static final String FUNC_GETDEPOSITREQUIREDFORFLOWRATE = "getDepositRequiredForFlowRate";

    public static final String FUNC_CREATEFLOW = "createFlow";

    public static final String FUNC_UPDATEFLOW = "updateFlow";

    public static final String FUNC_DELETEFLOW = "deleteFlow";

    public static final String FUNC_GETFLOW = "getFlow";

    public static final String FUNC_GETFLOWBYID = "getFlowByID";

    public static final String FUNC_GETACCOUNTFLOWINFO = "getAccountFlowInfo";

    public static final String FUNC_GETNETFLOW = "getNetFlow";

    public static final Event CODEUPDATED_EVENT = new Event("CodeUpdated",
            Arrays.asList(new TypeReference<Bytes32>() {
            }, new TypeReference<Address>() {
            }));

    public static final Event FLOWUPDATED_EVENT = new Event("FlowUpdated",
            Arrays.asList(new TypeReference<Address>(true) {
            }, new TypeReference<Address>(true) {
            }, new TypeReference<Address>(true) {
            }, new TypeReference<Int96>() {
            }, new TypeReference<Int256>() {
            }, new TypeReference<Int256>() {
            }, new TypeReference<DynamicBytes>() {
            }));

    @Deprecated
    protected ConstantFlowAgreementV1(String contractAddress, Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    protected ConstantFlowAgreementV1(String contractAddress, Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, credentials, contractGasProvider);
    }

    @Deprecated
    protected ConstantFlowAgreementV1(String contractAddress, Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    protected ConstantFlowAgreementV1(String contractAddress, Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, transactionManager, contractGasProvider);
    }

    @Deprecated
    public static ConstantFlowAgreementV1 load(String contractAddress, Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        return new ConstantFlowAgreementV1(contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    @Deprecated
    public static ConstantFlowAgreementV1 load(String contractAddress, Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        return new ConstantFlowAgreementV1(contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    public static ConstantFlowAgreementV1 load(String contractAddress, Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        return new ConstantFlowAgreementV1(contractAddress, web3j, credentials, contractGasProvider);
    }

    public static ConstantFlowAgreementV1 load(String contractAddress, Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        return new ConstantFlowAgreementV1(contractAddress, web3j, transactionManager, contractGasProvider);
    }

    public List<CodeUpdatedEventResponse> getCodeUpdatedEvents(TransactionReceipt transactionReceipt) {
        List<EventValuesWithLog> valueList = extractEventParametersWithLog(CODEUPDATED_EVENT, transactionReceipt);
        ArrayList<CodeUpdatedEventResponse> responses = new ArrayList<CodeUpdatedEventResponse>(valueList.size());
        for (EventValuesWithLog eventValues : valueList) {
            CodeUpdatedEventResponse typedResponse = new CodeUpdatedEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.uuid = (byte[]) eventValues.getNonIndexedValues().get(0).getValue();
            typedResponse.codeAddress = (String) eventValues.getNonIndexedValues().get(1).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public Flowable<CodeUpdatedEventResponse> codeUpdatedEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(new Function<Log, CodeUpdatedEventResponse>() {
            @Override
            public CodeUpdatedEventResponse apply(Log log) {
                EventValuesWithLog eventValues = extractEventParametersWithLog(CODEUPDATED_EVENT, log);
                CodeUpdatedEventResponse typedResponse = new CodeUpdatedEventResponse();
                typedResponse.log = log;
                typedResponse.uuid = (byte[]) eventValues.getNonIndexedValues().get(0).getValue();
                typedResponse.codeAddress = (String) eventValues.getNonIndexedValues().get(1).getValue();
                return typedResponse;
            }
        });
    }

    public Flowable<CodeUpdatedEventResponse> codeUpdatedEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(CODEUPDATED_EVENT));
        return codeUpdatedEventFlowable(filter);
    }

    public List<FlowUpdatedEventResponse> getFlowUpdatedEvents(TransactionReceipt transactionReceipt) {
        List<EventValuesWithLog> valueList = extractEventParametersWithLog(FLOWUPDATED_EVENT, transactionReceipt);
        ArrayList<FlowUpdatedEventResponse> responses = new ArrayList<FlowUpdatedEventResponse>(valueList.size());
        for (EventValuesWithLog eventValues : valueList) {
            FlowUpdatedEventResponse typedResponse = new FlowUpdatedEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.token = (String) eventValues.getIndexedValues().get(0).getValue();
            typedResponse.sender = (String) eventValues.getIndexedValues().get(1).getValue();
            typedResponse.receiver = (String) eventValues.getIndexedValues().get(2).getValue();
            typedResponse.flowRate = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
            typedResponse.totalSenderFlowRate = (BigInteger) eventValues.getNonIndexedValues().get(1).getValue();
            typedResponse.totalReceiverFlowRate = (BigInteger) eventValues.getNonIndexedValues().get(2).getValue();
            typedResponse.userData = (byte[]) eventValues.getNonIndexedValues().get(3).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public Flowable<FlowUpdatedEventResponse> flowUpdatedEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(new Function<Log, FlowUpdatedEventResponse>() {
            @Override
            public FlowUpdatedEventResponse apply(Log log) {
                EventValuesWithLog eventValues = extractEventParametersWithLog(FLOWUPDATED_EVENT, log);
                FlowUpdatedEventResponse typedResponse = new FlowUpdatedEventResponse();
                typedResponse.log = log;
                typedResponse.token = (String) eventValues.getIndexedValues().get(0).getValue();
                typedResponse.sender = (String) eventValues.getIndexedValues().get(1).getValue();
                typedResponse.receiver = (String) eventValues.getIndexedValues().get(2).getValue();
                typedResponse.flowRate = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
                typedResponse.totalSenderFlowRate = (BigInteger) eventValues.getNonIndexedValues().get(1).getValue();
                typedResponse.totalReceiverFlowRate = (BigInteger) eventValues.getNonIndexedValues().get(2).getValue();
                typedResponse.userData = (byte[]) eventValues.getNonIndexedValues().get(3).getValue();
                return typedResponse;
            }
        });
    }

    public Flowable<FlowUpdatedEventResponse> flowUpdatedEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(FLOWUPDATED_EVENT));
        return flowUpdatedEventFlowable(filter);
    }

    public RemoteFunctionCall<byte[]> agreementType() {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(FUNC_AGREEMENTTYPE,
                Arrays.asList(),
                Arrays.asList(new TypeReference<Bytes32>() {
                }));
        return executeRemoteCallSingleValueReturn(function, byte[].class);
    }

    public RemoteFunctionCall<String> getCodeAddress() {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(FUNC_GETCODEADDRESS,
                Arrays.asList(),
                Arrays.asList(new TypeReference<Address>() {
                }));
        return executeRemoteCallSingleValueReturn(function, String.class);
    }

    public RemoteFunctionCall<TransactionReceipt> initialize() {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(
                FUNC_INITIALIZE,
                Arrays.asList(),
                Collections.emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<byte[]> proxiableUUID() {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(FUNC_PROXIABLEUUID,
                Arrays.asList(),
                Arrays.asList(new TypeReference<Bytes32>() {
                }));
        return executeRemoteCallSingleValueReturn(function, byte[].class);
    }

    public RemoteFunctionCall<TransactionReceipt> updateCode(String newAddress) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(
                FUNC_UPDATECODE,
                Arrays.asList(new Address(160, newAddress)),
                Collections.emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<Tuple3<BigInteger, BigInteger, BigInteger>> realtimeBalanceOf(String token, String account, BigInteger time) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(FUNC_REALTIMEBALANCEOF,
                Arrays.asList(new Address(160, token),
                        new Address(160, account),
                        new Uint256(time)),
                Arrays.asList(new TypeReference<Int256>() {
                }, new TypeReference<Uint256>() {
                }, new TypeReference<Uint256>() {
                }));
        return new RemoteFunctionCall<Tuple3<BigInteger, BigInteger, BigInteger>>(function,
                new Callable<Tuple3<BigInteger, BigInteger, BigInteger>>() {
                    @Override
                    public Tuple3<BigInteger, BigInteger, BigInteger> call() throws Exception {
                        List<Type> results = executeCallMultipleValueReturn(function);
                        return new Tuple3<BigInteger, BigInteger, BigInteger>(
                                (BigInteger) results.get(0).getValue(),
                                (BigInteger) results.get(1).getValue(),
                                (BigInteger) results.get(2).getValue());
                    }
                });
    }

    public RemoteFunctionCall<BigInteger> getMaximumFlowRateFromDeposit(String token, BigInteger deposit) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(FUNC_GETMAXIMUMFLOWRATEFROMDEPOSIT,
                Arrays.asList(new Address(160, token),
                        new Uint256(deposit)),
                Arrays.asList(new TypeReference<Int96>() {
                }));
        return executeRemoteCallSingleValueReturn(function, BigInteger.class);
    }

    public RemoteFunctionCall<BigInteger> getDepositRequiredForFlowRate(String token, BigInteger flowRate) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(FUNC_GETDEPOSITREQUIREDFORFLOWRATE,
                Arrays.asList(new Address(160, token),
                        new Int96(flowRate)),
                Arrays.asList(new TypeReference<Uint256>() {
                }));
        return executeRemoteCallSingleValueReturn(function, BigInteger.class);
    }

    public RemoteFunctionCall<TransactionReceipt> createFlow(String token, String receiver, BigInteger flowRate, byte[] ctx) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(
                FUNC_CREATEFLOW,
                Arrays.asList(new Address(160, token),
                        new Address(160, receiver),
                        new Int96(flowRate),
                        new DynamicBytes(ctx)),
                Collections.emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> updateFlow(String token, String receiver, BigInteger flowRate, byte[] ctx) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(
                FUNC_UPDATEFLOW,
                Arrays.asList(new Address(160, token),
                        new Address(160, receiver),
                        new Int96(flowRate),
                        new DynamicBytes(ctx)),
                Collections.emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> deleteFlow(String token, String sender, String receiver, byte[] ctx) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(
                FUNC_DELETEFLOW,
                Arrays.asList(new Address(160, token),
                        new Address(160, sender),
                        new Address(160, receiver),
                        new DynamicBytes(ctx)),
                Collections.emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<Tuple4<BigInteger, BigInteger, BigInteger, BigInteger>> getFlow(String token, String sender, String receiver) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(FUNC_GETFLOW,
                Arrays.asList(new Address(160, token),
                        new Address(160, sender),
                        new Address(160, receiver)),
                Arrays.asList(new TypeReference<Uint256>() {
                }, new TypeReference<Int96>() {
                }, new TypeReference<Uint256>() {
                }, new TypeReference<Uint256>() {
                }));
        return new RemoteFunctionCall<Tuple4<BigInteger, BigInteger, BigInteger, BigInteger>>(function,
                new Callable<Tuple4<BigInteger, BigInteger, BigInteger, BigInteger>>() {
                    @Override
                    public Tuple4<BigInteger, BigInteger, BigInteger, BigInteger> call() throws Exception {
                        List<Type> results = executeCallMultipleValueReturn(function);
                        return new Tuple4<BigInteger, BigInteger, BigInteger, BigInteger>(
                                (BigInteger) results.get(0).getValue(),
                                (BigInteger) results.get(1).getValue(),
                                (BigInteger) results.get(2).getValue(),
                                (BigInteger) results.get(3).getValue());
                    }
                });
    }

    public RemoteFunctionCall<Tuple4<BigInteger, BigInteger, BigInteger, BigInteger>> getFlowByID(String token, byte[] flowId) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(FUNC_GETFLOWBYID,
                Arrays.asList(new Address(160, token),
                        new Bytes32(flowId)),
                Arrays.asList(new TypeReference<Uint256>() {
                }, new TypeReference<Int96>() {
                }, new TypeReference<Uint256>() {
                }, new TypeReference<Uint256>() {
                }));
        return new RemoteFunctionCall<Tuple4<BigInteger, BigInteger, BigInteger, BigInteger>>(function,
                new Callable<Tuple4<BigInteger, BigInteger, BigInteger, BigInteger>>() {
                    @Override
                    public Tuple4<BigInteger, BigInteger, BigInteger, BigInteger> call() throws Exception {
                        List<Type> results = executeCallMultipleValueReturn(function);
                        return new Tuple4<BigInteger, BigInteger, BigInteger, BigInteger>(
                                (BigInteger) results.get(0).getValue(),
                                (BigInteger) results.get(1).getValue(),
                                (BigInteger) results.get(2).getValue(),
                                (BigInteger) results.get(3).getValue());
                    }
                });
    }

    public RemoteFunctionCall<Tuple4<BigInteger, BigInteger, BigInteger, BigInteger>> getAccountFlowInfo(String token, String account) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(FUNC_GETACCOUNTFLOWINFO,
                Arrays.asList(new Address(160, token),
                        new Address(160, account)),
                Arrays.asList(new TypeReference<Uint256>() {
                }, new TypeReference<Int96>() {
                }, new TypeReference<Uint256>() {
                }, new TypeReference<Uint256>() {
                }));
        return new RemoteFunctionCall<Tuple4<BigInteger, BigInteger, BigInteger, BigInteger>>(function,
                new Callable<Tuple4<BigInteger, BigInteger, BigInteger, BigInteger>>() {
                    @Override
                    public Tuple4<BigInteger, BigInteger, BigInteger, BigInteger> call() throws Exception {
                        List<Type> results = executeCallMultipleValueReturn(function);
                        return new Tuple4<BigInteger, BigInteger, BigInteger, BigInteger>(
                                (BigInteger) results.get(0).getValue(),
                                (BigInteger) results.get(1).getValue(),
                                (BigInteger) results.get(2).getValue(),
                                (BigInteger) results.get(3).getValue());
                    }
                });
    }

    public RemoteFunctionCall<BigInteger> getNetFlow(String token, String account) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(
                FUNC_GETNETFLOW,
                Arrays.asList(new Address(160, token), new Address(160, account)),
                Arrays.asList(new TypeReference<Int96>() {
                }));
        return executeRemoteCallSingleValueReturn(function, BigInteger.class);
    }

    public static class CodeUpdatedEventResponse extends BaseEventResponse {
        public byte[] uuid;

        public String codeAddress;
    }

    public static class FlowUpdatedEventResponse extends BaseEventResponse {
        public String token;

        public String sender;

        public String receiver;

        public BigInteger flowRate;

        public BigInteger totalSenderFlowRate;

        public BigInteger totalReceiverFlowRate;

        public byte[] userData;
    }
}
