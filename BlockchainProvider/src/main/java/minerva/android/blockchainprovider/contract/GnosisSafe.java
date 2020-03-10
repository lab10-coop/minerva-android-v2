package minerva.android.blockchainprovider.contract;

import io.reactivex.Flowable;
import io.reactivex.functions.Function;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import org.web3j.abi.EventEncoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Bool;
import org.web3j.abi.datatypes.DynamicArray;
import org.web3j.abi.datatypes.DynamicBytes;
import org.web3j.abi.datatypes.Event;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.Utf8String;
import org.web3j.abi.datatypes.generated.Bytes32;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.RemoteFunctionCall;
import org.web3j.protocol.core.methods.request.EthFilter;
import org.web3j.protocol.core.methods.response.BaseEventResponse;
import org.web3j.protocol.core.methods.response.Log;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.tuples.generated.Tuple2;
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
public class GnosisSafe extends Contract {
    public static final String BINARY = "Bin file was not provided";

    public static final String FUNC_NAME = "NAME";

    public static final String FUNC_VERSION = "VERSION";

    public static final String FUNC_ADDOWNERWITHTHRESHOLD = "addOwnerWithThreshold";

    public static final String FUNC_APPROVEDHASHES = "approvedHashes";

    public static final String FUNC_CHANGEMASTERCOPY = "changeMasterCopy";

    public static final String FUNC_CHANGETHRESHOLD = "changeThreshold";

    public static final String FUNC_DISABLEMODULE = "disableModule";

    public static final String FUNC_DOMAINSEPARATOR = "domainSeparator";

    public static final String FUNC_ENABLEMODULE = "enableModule";

    public static final String FUNC_EXECTRANSACTIONFROMMODULE = "execTransactionFromModule";

    public static final String FUNC_EXECTRANSACTIONFROMMODULERETURNDATA = "execTransactionFromModuleReturnData";

    public static final String FUNC_GETBEACONADDRESS = "getBeaconAddress";

    public static final String FUNC_GETMODULES = "getModules";

    public static final String FUNC_GETMODULESPAGINATED = "getModulesPaginated";

    public static final String FUNC_GETOWNERS = "getOwners";

    public static final String FUNC_GETTHRESHOLD = "getThreshold";

    public static final String FUNC_ISOWNER = "isOwner";

    public static final String FUNC_NONCE = "nonce";

    public static final String FUNC_REMOVEOWNER = "removeOwner";

    public static final String FUNC_SETFALLBACKHANDLER = "setFallbackHandler";

    public static final String FUNC_SIGNEDMESSAGES = "signedMessages";

    public static final String FUNC_SWAPOWNER = "swapOwner";

    public static final String FUNC_SETUP = "setup";

    public static final String FUNC_SETUPWITHBEACON = "setupWithBeacon";

    public static final String FUNC_EXECTRANSACTION = "execTransaction";

    public static final String FUNC_REQUIREDTXGAS = "requiredTxGas";

    public static final String FUNC_APPROVEHASH = "approveHash";

    public static final String FUNC_SIGNMESSAGE = "signMessage";

    public static final String FUNC_ISVALIDSIGNATURE = "isValidSignature";

    public static final String FUNC_GETMESSAGEHASH = "getMessageHash";

    public static final String FUNC_ENCODETRANSACTIONDATA = "encodeTransactionData";

    public static final String FUNC_GETTRANSACTIONHASH = "getTransactionHash";

    public static final Event ADDEDOWNER_EVENT = new Event("AddedOwner", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}));
    ;

    public static final Event APPROVEHASH_EVENT = new Event("ApproveHash", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Bytes32>(true) {}, new TypeReference<Address>(true) {}));
    ;

    public static final Event CHANGEDMASTERCOPY_EVENT = new Event("ChangedMasterCopy", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}));
    ;

    public static final Event CHANGEDTHRESHOLD_EVENT = new Event("ChangedThreshold", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}));
    ;

    public static final Event DISABLEDMODULE_EVENT = new Event("DisabledModule", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}));
    ;

    public static final Event ENABLEDMODULE_EVENT = new Event("EnabledModule", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}));
    ;

    public static final Event EXECUTIONFAILURE_EVENT = new Event("ExecutionFailure", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Bytes32>() {}, new TypeReference<Uint256>() {}));
    ;

    public static final Event EXECUTIONFROMMODULEFAILURE_EVENT = new Event("ExecutionFromModuleFailure", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Address>(true) {}));
    ;

    public static final Event EXECUTIONFROMMODULESUCCESS_EVENT = new Event("ExecutionFromModuleSuccess", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Address>(true) {}));
    ;

    public static final Event EXECUTIONSUCCESS_EVENT = new Event("ExecutionSuccess", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Bytes32>() {}, new TypeReference<Uint256>() {}));
    ;

    public static final Event REMOVEDOWNER_EVENT = new Event("RemovedOwner", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}));
    ;

    public static final Event SIGNMSG_EVENT = new Event("SignMsg", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Bytes32>(true) {}));
    ;

    @Deprecated
    protected GnosisSafe(String contractAddress, Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    protected GnosisSafe(String contractAddress, Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, credentials, contractGasProvider);
    }

    @Deprecated
    protected GnosisSafe(String contractAddress, Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    protected GnosisSafe(String contractAddress, Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, transactionManager, contractGasProvider);
    }

    public List<AddedOwnerEventResponse> getAddedOwnerEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = extractEventParametersWithLog(ADDEDOWNER_EVENT, transactionReceipt);
        ArrayList<AddedOwnerEventResponse> responses = new ArrayList<AddedOwnerEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            AddedOwnerEventResponse typedResponse = new AddedOwnerEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.owner = (String) eventValues.getNonIndexedValues().get(0).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public Flowable<AddedOwnerEventResponse> addedOwnerEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(new Function<Log, AddedOwnerEventResponse>() {
            @Override
            public AddedOwnerEventResponse apply(Log log) {
                Contract.EventValuesWithLog eventValues = extractEventParametersWithLog(ADDEDOWNER_EVENT, log);
                AddedOwnerEventResponse typedResponse = new AddedOwnerEventResponse();
                typedResponse.log = log;
                typedResponse.owner = (String) eventValues.getNonIndexedValues().get(0).getValue();
                return typedResponse;
            }
        });
    }

    public Flowable<AddedOwnerEventResponse> addedOwnerEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(ADDEDOWNER_EVENT));
        return addedOwnerEventFlowable(filter);
    }

    public List<ApproveHashEventResponse> getApproveHashEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = extractEventParametersWithLog(APPROVEHASH_EVENT, transactionReceipt);
        ArrayList<ApproveHashEventResponse> responses = new ArrayList<ApproveHashEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            ApproveHashEventResponse typedResponse = new ApproveHashEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.approvedHash = (byte[]) eventValues.getIndexedValues().get(0).getValue();
            typedResponse.owner = (String) eventValues.getIndexedValues().get(1).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public Flowable<ApproveHashEventResponse> approveHashEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(new Function<Log, ApproveHashEventResponse>() {
            @Override
            public ApproveHashEventResponse apply(Log log) {
                Contract.EventValuesWithLog eventValues = extractEventParametersWithLog(APPROVEHASH_EVENT, log);
                ApproveHashEventResponse typedResponse = new ApproveHashEventResponse();
                typedResponse.log = log;
                typedResponse.approvedHash = (byte[]) eventValues.getIndexedValues().get(0).getValue();
                typedResponse.owner = (String) eventValues.getIndexedValues().get(1).getValue();
                return typedResponse;
            }
        });
    }

    public Flowable<ApproveHashEventResponse> approveHashEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(APPROVEHASH_EVENT));
        return approveHashEventFlowable(filter);
    }

    public List<ChangedMasterCopyEventResponse> getChangedMasterCopyEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = extractEventParametersWithLog(CHANGEDMASTERCOPY_EVENT, transactionReceipt);
        ArrayList<ChangedMasterCopyEventResponse> responses = new ArrayList<ChangedMasterCopyEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            ChangedMasterCopyEventResponse typedResponse = new ChangedMasterCopyEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.masterCopy = (String) eventValues.getNonIndexedValues().get(0).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public Flowable<ChangedMasterCopyEventResponse> changedMasterCopyEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(new Function<Log, ChangedMasterCopyEventResponse>() {
            @Override
            public ChangedMasterCopyEventResponse apply(Log log) {
                Contract.EventValuesWithLog eventValues = extractEventParametersWithLog(CHANGEDMASTERCOPY_EVENT, log);
                ChangedMasterCopyEventResponse typedResponse = new ChangedMasterCopyEventResponse();
                typedResponse.log = log;
                typedResponse.masterCopy = (String) eventValues.getNonIndexedValues().get(0).getValue();
                return typedResponse;
            }
        });
    }

    public Flowable<ChangedMasterCopyEventResponse> changedMasterCopyEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(CHANGEDMASTERCOPY_EVENT));
        return changedMasterCopyEventFlowable(filter);
    }

    public List<ChangedThresholdEventResponse> getChangedThresholdEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = extractEventParametersWithLog(CHANGEDTHRESHOLD_EVENT, transactionReceipt);
        ArrayList<ChangedThresholdEventResponse> responses = new ArrayList<ChangedThresholdEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            ChangedThresholdEventResponse typedResponse = new ChangedThresholdEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.threshold = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public Flowable<ChangedThresholdEventResponse> changedThresholdEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(new Function<Log, ChangedThresholdEventResponse>() {
            @Override
            public ChangedThresholdEventResponse apply(Log log) {
                Contract.EventValuesWithLog eventValues = extractEventParametersWithLog(CHANGEDTHRESHOLD_EVENT, log);
                ChangedThresholdEventResponse typedResponse = new ChangedThresholdEventResponse();
                typedResponse.log = log;
                typedResponse.threshold = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
                return typedResponse;
            }
        });
    }

    public Flowable<ChangedThresholdEventResponse> changedThresholdEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(CHANGEDTHRESHOLD_EVENT));
        return changedThresholdEventFlowable(filter);
    }

    public List<DisabledModuleEventResponse> getDisabledModuleEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = extractEventParametersWithLog(DISABLEDMODULE_EVENT, transactionReceipt);
        ArrayList<DisabledModuleEventResponse> responses = new ArrayList<DisabledModuleEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            DisabledModuleEventResponse typedResponse = new DisabledModuleEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.module = (String) eventValues.getNonIndexedValues().get(0).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public Flowable<DisabledModuleEventResponse> disabledModuleEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(new Function<Log, DisabledModuleEventResponse>() {
            @Override
            public DisabledModuleEventResponse apply(Log log) {
                Contract.EventValuesWithLog eventValues = extractEventParametersWithLog(DISABLEDMODULE_EVENT, log);
                DisabledModuleEventResponse typedResponse = new DisabledModuleEventResponse();
                typedResponse.log = log;
                typedResponse.module = (String) eventValues.getNonIndexedValues().get(0).getValue();
                return typedResponse;
            }
        });
    }

    public Flowable<DisabledModuleEventResponse> disabledModuleEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(DISABLEDMODULE_EVENT));
        return disabledModuleEventFlowable(filter);
    }

    public List<EnabledModuleEventResponse> getEnabledModuleEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = extractEventParametersWithLog(ENABLEDMODULE_EVENT, transactionReceipt);
        ArrayList<EnabledModuleEventResponse> responses = new ArrayList<EnabledModuleEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            EnabledModuleEventResponse typedResponse = new EnabledModuleEventResponse();
//            typedResponse.log = eventValues.getLog();
            typedResponse.module = (String) eventValues.getNonIndexedValues().get(0).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public Flowable<EnabledModuleEventResponse> enabledModuleEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(new Function<Log, EnabledModuleEventResponse>() {
            @Override
            public EnabledModuleEventResponse apply(Log log) {
                Contract.EventValuesWithLog eventValues = extractEventParametersWithLog(ENABLEDMODULE_EVENT, log);
                EnabledModuleEventResponse typedResponse = new EnabledModuleEventResponse();
                typedResponse.log = log;
                typedResponse.module = (String) eventValues.getNonIndexedValues().get(0).getValue();
                return typedResponse;
            }
        });
    }

    public Flowable<EnabledModuleEventResponse> enabledModuleEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(ENABLEDMODULE_EVENT));
        return enabledModuleEventFlowable(filter);
    }

    public List<ExecutionFailureEventResponse> getExecutionFailureEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = extractEventParametersWithLog(EXECUTIONFAILURE_EVENT, transactionReceipt);
        ArrayList<ExecutionFailureEventResponse> responses = new ArrayList<ExecutionFailureEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            ExecutionFailureEventResponse typedResponse = new ExecutionFailureEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.txHash = (byte[]) eventValues.getNonIndexedValues().get(0).getValue();
            typedResponse.payment = (BigInteger) eventValues.getNonIndexedValues().get(1).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public Flowable<ExecutionFailureEventResponse> executionFailureEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(new Function<Log, ExecutionFailureEventResponse>() {
            @Override
            public ExecutionFailureEventResponse apply(Log log) {
                Contract.EventValuesWithLog eventValues = extractEventParametersWithLog(EXECUTIONFAILURE_EVENT, log);
                ExecutionFailureEventResponse typedResponse = new ExecutionFailureEventResponse();
                typedResponse.log = log;
                typedResponse.txHash = (byte[]) eventValues.getNonIndexedValues().get(0).getValue();
                typedResponse.payment = (BigInteger) eventValues.getNonIndexedValues().get(1).getValue();
                return typedResponse;
            }
        });
    }

    public Flowable<ExecutionFailureEventResponse> executionFailureEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(EXECUTIONFAILURE_EVENT));
        return executionFailureEventFlowable(filter);
    }

    public List<ExecutionFromModuleFailureEventResponse> getExecutionFromModuleFailureEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = extractEventParametersWithLog(EXECUTIONFROMMODULEFAILURE_EVENT, transactionReceipt);
        ArrayList<ExecutionFromModuleFailureEventResponse> responses = new ArrayList<ExecutionFromModuleFailureEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            ExecutionFromModuleFailureEventResponse typedResponse = new ExecutionFromModuleFailureEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.module = (String) eventValues.getIndexedValues().get(0).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public Flowable<ExecutionFromModuleFailureEventResponse> executionFromModuleFailureEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(new Function<Log, ExecutionFromModuleFailureEventResponse>() {
            @Override
            public ExecutionFromModuleFailureEventResponse apply(Log log) {
                Contract.EventValuesWithLog eventValues = extractEventParametersWithLog(EXECUTIONFROMMODULEFAILURE_EVENT, log);
                ExecutionFromModuleFailureEventResponse typedResponse = new ExecutionFromModuleFailureEventResponse();
                typedResponse.log = log;
                typedResponse.module = (String) eventValues.getIndexedValues().get(0).getValue();
                return typedResponse;
            }
        });
    }

    public Flowable<ExecutionFromModuleFailureEventResponse> executionFromModuleFailureEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(EXECUTIONFROMMODULEFAILURE_EVENT));
        return executionFromModuleFailureEventFlowable(filter);
    }

    public List<ExecutionFromModuleSuccessEventResponse> getExecutionFromModuleSuccessEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = extractEventParametersWithLog(EXECUTIONFROMMODULESUCCESS_EVENT, transactionReceipt);
        ArrayList<ExecutionFromModuleSuccessEventResponse> responses = new ArrayList<ExecutionFromModuleSuccessEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            ExecutionFromModuleSuccessEventResponse typedResponse = new ExecutionFromModuleSuccessEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.module = (String) eventValues.getIndexedValues().get(0).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public Flowable<ExecutionFromModuleSuccessEventResponse> executionFromModuleSuccessEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(new Function<Log, ExecutionFromModuleSuccessEventResponse>() {
            @Override
            public ExecutionFromModuleSuccessEventResponse apply(Log log) {
                Contract.EventValuesWithLog eventValues = extractEventParametersWithLog(EXECUTIONFROMMODULESUCCESS_EVENT, log);
                ExecutionFromModuleSuccessEventResponse typedResponse = new ExecutionFromModuleSuccessEventResponse();
                typedResponse.log = log;
                typedResponse.module = (String) eventValues.getIndexedValues().get(0).getValue();
                return typedResponse;
            }
        });
    }

    public Flowable<ExecutionFromModuleSuccessEventResponse> executionFromModuleSuccessEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(EXECUTIONFROMMODULESUCCESS_EVENT));
        return executionFromModuleSuccessEventFlowable(filter);
    }

    public List<ExecutionSuccessEventResponse> getExecutionSuccessEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = extractEventParametersWithLog(EXECUTIONSUCCESS_EVENT, transactionReceipt);
        ArrayList<ExecutionSuccessEventResponse> responses = new ArrayList<ExecutionSuccessEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            ExecutionSuccessEventResponse typedResponse = new ExecutionSuccessEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.txHash = (byte[]) eventValues.getNonIndexedValues().get(0).getValue();
            typedResponse.payment = (BigInteger) eventValues.getNonIndexedValues().get(1).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public Flowable<ExecutionSuccessEventResponse> executionSuccessEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(new Function<Log, ExecutionSuccessEventResponse>() {
            @Override
            public ExecutionSuccessEventResponse apply(Log log) {
                Contract.EventValuesWithLog eventValues = extractEventParametersWithLog(EXECUTIONSUCCESS_EVENT, log);
                ExecutionSuccessEventResponse typedResponse = new ExecutionSuccessEventResponse();
                typedResponse.log = log;
                typedResponse.txHash = (byte[]) eventValues.getNonIndexedValues().get(0).getValue();
                typedResponse.payment = (BigInteger) eventValues.getNonIndexedValues().get(1).getValue();
                return typedResponse;
            }
        });
    }

    public Flowable<ExecutionSuccessEventResponse> executionSuccessEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(EXECUTIONSUCCESS_EVENT));
        return executionSuccessEventFlowable(filter);
    }

    public List<RemovedOwnerEventResponse> getRemovedOwnerEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = extractEventParametersWithLog(REMOVEDOWNER_EVENT, transactionReceipt);
        ArrayList<RemovedOwnerEventResponse> responses = new ArrayList<RemovedOwnerEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            RemovedOwnerEventResponse typedResponse = new RemovedOwnerEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.owner = (String) eventValues.getNonIndexedValues().get(0).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public Flowable<RemovedOwnerEventResponse> removedOwnerEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(new Function<Log, RemovedOwnerEventResponse>() {
            @Override
            public RemovedOwnerEventResponse apply(Log log) {
                Contract.EventValuesWithLog eventValues = extractEventParametersWithLog(REMOVEDOWNER_EVENT, log);
                RemovedOwnerEventResponse typedResponse = new RemovedOwnerEventResponse();
                typedResponse.log = log;
                typedResponse.owner = (String) eventValues.getNonIndexedValues().get(0).getValue();
                return typedResponse;
            }
        });
    }

    public Flowable<RemovedOwnerEventResponse> removedOwnerEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(REMOVEDOWNER_EVENT));
        return removedOwnerEventFlowable(filter);
    }

    public List<SignMsgEventResponse> getSignMsgEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = extractEventParametersWithLog(SIGNMSG_EVENT, transactionReceipt);
        ArrayList<SignMsgEventResponse> responses = new ArrayList<SignMsgEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            SignMsgEventResponse typedResponse = new SignMsgEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.msgHash = (byte[]) eventValues.getIndexedValues().get(0).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public Flowable<SignMsgEventResponse> signMsgEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(new Function<Log, SignMsgEventResponse>() {
            @Override
            public SignMsgEventResponse apply(Log log) {
                Contract.EventValuesWithLog eventValues = extractEventParametersWithLog(SIGNMSG_EVENT, log);
                SignMsgEventResponse typedResponse = new SignMsgEventResponse();
                typedResponse.log = log;
                typedResponse.msgHash = (byte[]) eventValues.getIndexedValues().get(0).getValue();
                return typedResponse;
            }
        });
    }

    public Flowable<SignMsgEventResponse> signMsgEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(SIGNMSG_EVENT));
        return signMsgEventFlowable(filter);
    }

    public RemoteFunctionCall<String> NAME() {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(FUNC_NAME, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Utf8String>() {}));
        return executeRemoteCallSingleValueReturn(function, String.class);
    }

    public RemoteFunctionCall<String> VERSION() {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(FUNC_VERSION, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Utf8String>() {}));
        return executeRemoteCallSingleValueReturn(function, String.class);
    }

    public RemoteFunctionCall<TransactionReceipt> addOwnerWithThreshold(String owner, BigInteger _threshold) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(
                FUNC_ADDOWNERWITHTHRESHOLD, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(160, owner), 
                new org.web3j.abi.datatypes.generated.Uint256(_threshold)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<BigInteger> approvedHashes(String param0, byte[] param1) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(FUNC_APPROVEDHASHES, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(160, param0), 
                new org.web3j.abi.datatypes.generated.Bytes32(param1)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}));
        return executeRemoteCallSingleValueReturn(function, BigInteger.class);
    }

    public RemoteFunctionCall<TransactionReceipt> changeMasterCopy(String _masterCopy) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(
                FUNC_CHANGEMASTERCOPY, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(160, _masterCopy)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> changeThreshold(BigInteger _threshold) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(
                FUNC_CHANGETHRESHOLD, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(_threshold)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> disableModule(String prevModule, String module) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(
                FUNC_DISABLEMODULE, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(160, prevModule), 
                new org.web3j.abi.datatypes.Address(160, module)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<byte[]> domainSeparator() {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(FUNC_DOMAINSEPARATOR, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Bytes32>() {}));
        return executeRemoteCallSingleValueReturn(function, byte[].class);
    }

    public RemoteFunctionCall<TransactionReceipt> enableModule(String module) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(
                FUNC_ENABLEMODULE, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(160, module)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> execTransactionFromModule(String to, BigInteger value, byte[] data, BigInteger operation) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(
                FUNC_EXECTRANSACTIONFROMMODULE, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(160, to), 
                new org.web3j.abi.datatypes.generated.Uint256(value), 
                new org.web3j.abi.datatypes.DynamicBytes(data), 
                new org.web3j.abi.datatypes.generated.Uint8(operation)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> execTransactionFromModuleReturnData(String to, BigInteger value, byte[] data, BigInteger operation) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(
                FUNC_EXECTRANSACTIONFROMMODULERETURNDATA, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(160, to), 
                new org.web3j.abi.datatypes.generated.Uint256(value), 
                new org.web3j.abi.datatypes.DynamicBytes(data), 
                new org.web3j.abi.datatypes.generated.Uint8(operation)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<String> getBeaconAddress() {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(FUNC_GETBEACONADDRESS, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}));
        return executeRemoteCallSingleValueReturn(function, String.class);
    }

    public RemoteFunctionCall<List> getModules() {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(FUNC_GETMODULES, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<DynamicArray<Address>>() {}));
        return new RemoteFunctionCall<List>(function,
                new Callable<List>() {
                    @Override
                    @SuppressWarnings("unchecked")
                    public List call() throws Exception {
                        List<Type> result = (List<Type>) executeCallSingleValueReturn(function, List.class);
                        return convertToNative(result);
                    }
                });
    }

    public RemoteFunctionCall<List> getOwners() {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(FUNC_GETOWNERS, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<DynamicArray<Address>>() {}));
        return new RemoteFunctionCall<List>(function,
                new Callable<List>() {
                    @Override
                    @SuppressWarnings("unchecked")
                    public List call() throws Exception {
                        List<Type> result = (List<Type>) executeCallSingleValueReturn(function, List.class);
                        return convertToNative(result);
                    }
                });
    }

    public RemoteFunctionCall<BigInteger> getThreshold() {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(FUNC_GETTHRESHOLD, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}));
        return executeRemoteCallSingleValueReturn(function, BigInteger.class);
    }

    public RemoteFunctionCall<Boolean> isOwner(String owner) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(FUNC_ISOWNER, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(160, owner)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Bool>() {}));
        return executeRemoteCallSingleValueReturn(function, Boolean.class);
    }

    public RemoteFunctionCall<BigInteger> nonce() {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(FUNC_NONCE, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}));
        return executeRemoteCallSingleValueReturn(function, BigInteger.class);
    }

    public RemoteFunctionCall<TransactionReceipt> removeOwner(String prevOwner, String owner, BigInteger _threshold) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(
                FUNC_REMOVEOWNER, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(160, prevOwner), 
                new org.web3j.abi.datatypes.Address(160, owner), 
                new org.web3j.abi.datatypes.generated.Uint256(_threshold)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> setFallbackHandler(String handler) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(
                FUNC_SETFALLBACKHANDLER, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(160, handler)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<BigInteger> signedMessages(byte[] param0) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(FUNC_SIGNEDMESSAGES, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Bytes32(param0)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}));
        return executeRemoteCallSingleValueReturn(function, BigInteger.class);
    }

    public RemoteFunctionCall<TransactionReceipt> swapOwner(String prevOwner, String oldOwner, String newOwner) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(
                FUNC_SWAPOWNER, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(160, prevOwner), 
                new org.web3j.abi.datatypes.Address(160, oldOwner), 
                new org.web3j.abi.datatypes.Address(160, newOwner)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> setup(List<String> _owners, BigInteger _threshold, String to, byte[] data, String fallbackHandler, String paymentToken, BigInteger payment, String paymentReceiver) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(
                FUNC_SETUP, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.DynamicArray<org.web3j.abi.datatypes.Address>(
                        org.web3j.abi.datatypes.Address.class,
                        org.web3j.abi.Utils.typeMap(_owners, org.web3j.abi.datatypes.Address.class)), 
                new org.web3j.abi.datatypes.generated.Uint256(_threshold), 
                new org.web3j.abi.datatypes.Address(160, to), 
                new org.web3j.abi.datatypes.DynamicBytes(data), 
                new org.web3j.abi.datatypes.Address(160, fallbackHandler), 
                new org.web3j.abi.datatypes.Address(160, paymentToken), 
                new org.web3j.abi.datatypes.generated.Uint256(payment), 
                new org.web3j.abi.datatypes.Address(160, paymentReceiver)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> setupWithBeacon(List<String> _owners, BigInteger _threshold, String to, byte[] data, String fallbackHandler, String paymentToken, BigInteger payment, String paymentReceiver, String beaconContract) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(
                FUNC_SETUPWITHBEACON, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.DynamicArray<org.web3j.abi.datatypes.Address>(
                        org.web3j.abi.datatypes.Address.class,
                        org.web3j.abi.Utils.typeMap(_owners, org.web3j.abi.datatypes.Address.class)), 
                new org.web3j.abi.datatypes.generated.Uint256(_threshold), 
                new org.web3j.abi.datatypes.Address(160, to), 
                new org.web3j.abi.datatypes.DynamicBytes(data), 
                new org.web3j.abi.datatypes.Address(160, fallbackHandler), 
                new org.web3j.abi.datatypes.Address(160, paymentToken), 
                new org.web3j.abi.datatypes.generated.Uint256(payment), 
                new org.web3j.abi.datatypes.Address(160, paymentReceiver), 
                new org.web3j.abi.datatypes.Address(160, beaconContract)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> execTransaction(String to, BigInteger value, byte[] data, BigInteger operation, BigInteger safeTxGas, BigInteger baseGas, BigInteger gasPrice, String gasToken, String refundReceiver, byte[] signatures) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(
                FUNC_EXECTRANSACTION, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(160, to), 
                new org.web3j.abi.datatypes.generated.Uint256(value), 
                new org.web3j.abi.datatypes.DynamicBytes(data), 
                new org.web3j.abi.datatypes.generated.Uint8(operation), 
                new org.web3j.abi.datatypes.generated.Uint256(safeTxGas), 
                new org.web3j.abi.datatypes.generated.Uint256(baseGas), 
                new org.web3j.abi.datatypes.generated.Uint256(gasPrice), 
                new org.web3j.abi.datatypes.Address(160, gasToken), 
                new org.web3j.abi.datatypes.Address(160, refundReceiver), 
                new org.web3j.abi.datatypes.DynamicBytes(signatures)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> requiredTxGas(String to, BigInteger value, byte[] data, BigInteger operation) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(
                FUNC_REQUIREDTXGAS, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(160, to), 
                new org.web3j.abi.datatypes.generated.Uint256(value), 
                new org.web3j.abi.datatypes.DynamicBytes(data), 
                new org.web3j.abi.datatypes.generated.Uint8(operation)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> approveHash(byte[] hashToApprove) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(
                FUNC_APPROVEHASH, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Bytes32(hashToApprove)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> signMessage(byte[] _data) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(
                FUNC_SIGNMESSAGE, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.DynamicBytes(_data)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> isValidSignature(byte[] _data, byte[] _signature) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(
                FUNC_ISVALIDSIGNATURE, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.DynamicBytes(_data), 
                new org.web3j.abi.datatypes.DynamicBytes(_signature)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<byte[]> getMessageHash(byte[] message) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(FUNC_GETMESSAGEHASH, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.DynamicBytes(message)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Bytes32>() {}));
        return executeRemoteCallSingleValueReturn(function, byte[].class);
    }

    public RemoteFunctionCall<byte[]> encodeTransactionData(String to, BigInteger value, byte[] data, BigInteger operation, BigInteger safeTxGas, BigInteger baseGas, BigInteger gasPrice, String gasToken, String refundReceiver, BigInteger _nonce) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(FUNC_ENCODETRANSACTIONDATA, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(160, to), 
                new org.web3j.abi.datatypes.generated.Uint256(value), 
                new org.web3j.abi.datatypes.DynamicBytes(data), 
                new org.web3j.abi.datatypes.generated.Uint8(operation), 
                new org.web3j.abi.datatypes.generated.Uint256(safeTxGas), 
                new org.web3j.abi.datatypes.generated.Uint256(baseGas), 
                new org.web3j.abi.datatypes.generated.Uint256(gasPrice), 
                new org.web3j.abi.datatypes.Address(160, gasToken), 
                new org.web3j.abi.datatypes.Address(160, refundReceiver), 
                new org.web3j.abi.datatypes.generated.Uint256(_nonce)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<DynamicBytes>() {}));
        return executeRemoteCallSingleValueReturn(function, byte[].class);
    }

    public RemoteFunctionCall<byte[]> getTransactionHash(String to, BigInteger value, byte[] data, BigInteger operation, BigInteger safeTxGas, BigInteger baseGas, BigInteger gasPrice, String gasToken, String refundReceiver, BigInteger _nonce) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(FUNC_GETTRANSACTIONHASH, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(160, to), 
                new org.web3j.abi.datatypes.generated.Uint256(value), 
                new org.web3j.abi.datatypes.DynamicBytes(data), 
                new org.web3j.abi.datatypes.generated.Uint8(operation), 
                new org.web3j.abi.datatypes.generated.Uint256(safeTxGas), 
                new org.web3j.abi.datatypes.generated.Uint256(baseGas), 
                new org.web3j.abi.datatypes.generated.Uint256(gasPrice), 
                new org.web3j.abi.datatypes.Address(160, gasToken), 
                new org.web3j.abi.datatypes.Address(160, refundReceiver), 
                new org.web3j.abi.datatypes.generated.Uint256(_nonce)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Bytes32>() {}));
        return executeRemoteCallSingleValueReturn(function, byte[].class);
    }

    @Deprecated
    public static GnosisSafe load(String contractAddress, Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        return new GnosisSafe(contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    @Deprecated
    public static GnosisSafe load(String contractAddress, Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        return new GnosisSafe(contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    public static GnosisSafe load(String contractAddress, Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        return new GnosisSafe(contractAddress, web3j, credentials, contractGasProvider);
    }

    public static GnosisSafe load(String contractAddress, Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        return new GnosisSafe(contractAddress, web3j, transactionManager, contractGasProvider);
    }

    public static class AddedOwnerEventResponse extends BaseEventResponse {
        public String owner;
    }

    public static class ApproveHashEventResponse extends BaseEventResponse {
        public byte[] approvedHash;

        public String owner;
    }

    public static class ChangedMasterCopyEventResponse extends BaseEventResponse {
        public String masterCopy;
    }

    public static class ChangedThresholdEventResponse extends BaseEventResponse {
        public BigInteger threshold;
    }

    public static class DisabledModuleEventResponse extends BaseEventResponse {
        public String module;
    }

    public static class EnabledModuleEventResponse extends BaseEventResponse {
        public String module;
    }

    public static class ExecutionFailureEventResponse extends BaseEventResponse {
        public byte[] txHash;

        public BigInteger payment;
    }

    public static class ExecutionFromModuleFailureEventResponse extends BaseEventResponse {
        public String module;
    }

    public static class ExecutionFromModuleSuccessEventResponse extends BaseEventResponse {
        public String module;
    }

    public static class ExecutionSuccessEventResponse extends BaseEventResponse {
        public byte[] txHash;

        public BigInteger payment;
    }

    public static class RemovedOwnerEventResponse extends BaseEventResponse {
        public String owner;
    }

    public static class SignMsgEventResponse extends BaseEventResponse {
        public byte[] msgHash;
    }
}
