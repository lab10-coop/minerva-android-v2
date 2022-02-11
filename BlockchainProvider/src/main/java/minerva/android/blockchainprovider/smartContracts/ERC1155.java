package minerva.android.blockchainprovider.smartContracts;

import io.reactivex.Flowable;
import io.reactivex.functions.Function;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Callable;
import org.web3j.abi.EventEncoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Bool;
import org.web3j.abi.datatypes.DynamicArray;
import org.web3j.abi.datatypes.Event;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.Utf8String;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.RemoteCall;
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
 * <p>Generated with web3j version 1.4.1.
 */
@SuppressWarnings("rawtypes")
public class ERC1155 extends Contract {
    public static final String BINARY = "0x608060405234801561001057600080fd5b506118f2806100206000396000f3fe608060405234801561001057600080fd5b506004361061007c5760003560e01c80634e1273f41161005b5780634e1273f4146102ab578063a22cb465146103ce578063e985e9c51461041e578063f242432a1461049a5761007c565b8062fdd58e1461008157806301ffc9a7146100e35780632eb2c2d614610148575b600080fd5b6100cd6004803603604081101561009757600080fd5b81019080803573ffffffffffffffffffffffffffffffffffffffff16906020019092919080359060200190929190505050610567565b6040518082815260200191505060405180910390f35b61012e600480360360208110156100f957600080fd5b8101908080357bffffffffffffffffffffffffffffffffffffffffffffffffffffffff191690602001909291905050506105c1565b604051808215151515815260200191505060405180910390f35b6102a9600480360360a081101561015e57600080fd5b81019080803573ffffffffffffffffffffffffffffffffffffffff169060200190929190803573ffffffffffffffffffffffffffffffffffffffff169060200190929190803590602001906401000000008111156101bb57600080fd5b8201836020820111156101cd57600080fd5b803590602001918460208302840111640100000000831117156101ef57600080fd5b90919293919293908035906020019064010000000081111561021057600080fd5b82018360208201111561022257600080fd5b8035906020019184602083028401116401000000008311171561024457600080fd5b90919293919293908035906020019064010000000081111561026557600080fd5b82018360208201111561027757600080fd5b8035906020019184600183028401116401000000008311171561029957600080fd5b9091929391929390505050610672565b005b610377600480360360408110156102c157600080fd5b81019080803590602001906401000000008111156102de57600080fd5b8201836020820111156102f057600080fd5b8035906020019184602083028401116401000000008311171561031257600080fd5b90919293919293908035906020019064010000000081111561033357600080fd5b82018360208201111561034557600080fd5b8035906020019184602083028401116401000000008311171561036757600080fd5b9091929391929390505050610c0e565b6040518080602001828103825283818151815260200191508051906020019060200280838360005b838110156103ba57808201518184015260208101905061039f565b505050509050019250505060405180910390f35b61041c600480360360408110156103e457600080fd5b81019080803573ffffffffffffffffffffffffffffffffffffffff169060200190929190803515159060200190929190505050610d24565b005b6104806004803603604081101561043457600080fd5b81019080803573ffffffffffffffffffffffffffffffffffffffff169060200190929190803573ffffffffffffffffffffffffffffffffffffffff169060200190929190505050610e25565b604051808215151515815260200191505060405180910390f35b610565600480360360a08110156104b057600080fd5b81019080803573ffffffffffffffffffffffffffffffffffffffff169060200190929190803573ffffffffffffffffffffffffffffffffffffffff16906020019092919080359060200190929190803590602001909291908035906020019064010000000081111561052157600080fd5b82018360208201111561053357600080fd5b8035906020019184600183028401116401000000008311171561055557600080fd5b9091929391929390505050610eb9565b005b600080600083815260200190815260200160002060008473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff16815260200190815260200160002054905092915050565b60006301ffc9a760e01b7bffffffffffffffffffffffffffffffffffffffffffffffffffffffff1916827bffffffffffffffffffffffffffffffffffffffffffffffffffffffff1916148061065a575063d9b67a2660e01b7bffffffffffffffffffffffffffffffffffffffffffffffffffffffff1916827bffffffffffffffffffffffffffffffffffffffffffffffffffffffff1916145b15610668576001905061066d565b600090505b919050565b600073ffffffffffffffffffffffffffffffffffffffff168773ffffffffffffffffffffffffffffffffffffffff1614156106f8576040517f08c379a00000000000000000000000000000000000000000000000000000000081526004018080602001828103825260258152602001806117ca6025913960400191505060405180910390fd5b838390508686905014610756576040517f08c379a000000000000000000000000000000000000000000000000000000000815260040180806020018281038252602981526020018061185c6029913960400191505060405180910390fd5b3373ffffffffffffffffffffffffffffffffffffffff168873ffffffffffffffffffffffffffffffffffffffff16148061081d575060011515600160008a73ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff16815260200190815260200160002060003373ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff16815260200190815260200160002060009054906101000a900460ff161515145b610872576040517f08c379a000000000000000000000000000000000000000000000000000000000815260040180806020018281038252602f8152602001806117ef602f913960400191505060405180910390fd5b60008090505b86869050811015610a2857600087878381811061089157fe5b90506020020135905060008686848181106108a857fe5b9050602002013590506109138160008085815260200190815260200160002060008e73ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff168152602001908152602001600020546112e590919063ffffffff16565b60008084815260200190815260200160002060008d73ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff168152602001908152602001600020819055506109c860008084815260200190815260200160002060008c73ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff16815260200190815260200160002054826112fc90919063ffffffff16565b60008084815260200190815260200160002060008c73ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff168152602001908152602001600020819055505050806001019050610878565b508673ffffffffffffffffffffffffffffffffffffffff168873ffffffffffffffffffffffffffffffffffffffff163373ffffffffffffffffffffffffffffffffffffffff167f4a39dc06d4c0dbc64b70af90fd698a233a518aa5d07e595d983b8c0526c8f7fb898989896040518080602001806020018381038352878782818152602001925060200280828437600081840152601f19601f8201169050808301925050508381038252858582818152602001925060200280828437600081840152601f19601f820116905080830192505050965050505050505060405180910390a4610b2a8773ffffffffffffffffffffffffffffffffffffffff16611316565b15610c0457610c03338989898980806020026020016040519081016040528093929190818152602001838360200280828437600081840152601f19601f82011690508083019250505050505050888880806020026020016040519081016040528093929190818152602001838360200280828437600081840152601f19601f8201169050808301925050505050505087878080601f016020809104026020016040519081016040528093929190818152602001838380828437600081840152601f19601f82011690508083019250505050505050611329565b5b5050505050505050565b6060828290508585905014610c2257600080fd5b606085859050604051908082528060200260200182016040528015610c565781602001602082028038833980820191505090505b50905060008090505b86869050811015610d1757600080868684818110610c7957fe5b9050602002013581526020019081526020016000206000888884818110610c9c57fe5b9050602002013573ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff16815260200190815260200160002054828281518110610d0057fe5b602002602001018181525050806001019050610c5f565b5080915050949350505050565b80600160003373ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff16815260200190815260200160002060008473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff16815260200190815260200160002060006101000a81548160ff0219169083151502179055508173ffffffffffffffffffffffffffffffffffffffff163373ffffffffffffffffffffffffffffffffffffffff167f17307eab39ab6107e8899845ad3d59bd9653f200f220920489ca2b5937696c3183604051808215151515815260200191505060405180910390a35050565b6000600160008473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff16815260200190815260200160002060008373ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff16815260200190815260200160002060009054906101000a900460ff16905092915050565b600073ffffffffffffffffffffffffffffffffffffffff168573ffffffffffffffffffffffffffffffffffffffff161415610f5c576040517f08c379a00000000000000000000000000000000000000000000000000000000081526004018080602001828103825260158152602001807f5f746f206d757374206265206e6f6e2d7a65726f2e000000000000000000000081525060200191505060405180910390fd5b3373ffffffffffffffffffffffffffffffffffffffff168673ffffffffffffffffffffffffffffffffffffffff161480611023575060011515600160008873ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff16815260200190815260200160002060003373ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff16815260200190815260200160002060009054906101000a900460ff161515145b611078576040517f08c379a000000000000000000000000000000000000000000000000000000000815260040180806020018281038252602f8152602001806117ef602f913960400191505060405180910390fd5b6110da8360008087815260200190815260200160002060008973ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff168152602001908152602001600020546112e590919063ffffffff16565b60008086815260200190815260200160002060008873ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1681526020019081526020016000208190555061118f60008086815260200190815260200160002060008773ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff16815260200190815260200160002054846112fc90919063ffffffff16565b60008086815260200190815260200160002060008773ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff168152602001908152602001600020819055508473ffffffffffffffffffffffffffffffffffffffff168673ffffffffffffffffffffffffffffffffffffffff163373ffffffffffffffffffffffffffffffffffffffff167fc3d58168c5ae7397731d063d5bbf3d657854427343f4c083240f7aacaa2d0f628787604051808381526020018281526020019250505060405180910390a46112858573ffffffffffffffffffffffffffffffffffffffff16611316565b156112dd576112dc338787878787878080601f016020809104026020016040519081016040528093929190818152602001838380828437600081840152601f19601f820116905080830192505050505050506115ba565b5b505050505050565b6000828211156112f157fe5b818303905092915050565b600081830190508281101561130d57fe5b80905092915050565b600080823b905060008111915050919050565b63bc197c8160e01b7bffffffffffffffffffffffffffffffffffffffffffffffffffffffff19168473ffffffffffffffffffffffffffffffffffffffff1663bc197c8188888787876040518663ffffffff1660e01b8152600401808673ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1681526020018573ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff168152602001806020018060200180602001848103845287818151815260200191508051906020019060200280838360005b8381101561142f578082015181840152602081019050611414565b50505050905001848103835286818151815260200191508051906020019060200280838360005b83811015611471578082015181840152602081019050611456565b50505050905001848103825285818151815260200191508051906020019080838360005b838110156114b0578082015181840152602081019050611495565b50505050905090810190601f1680156114dd5780820380516001836020036101000a031916815260200191505b5098505050505050505050602060405180830381600087803b15801561150257600080fd5b505af1158015611516573d6000803e3d6000fd5b505050506040513d602081101561152c57600080fd5b81019080805190602001909291905050507bffffffffffffffffffffffffffffffffffffffffffffffffffffffff1916146115b2576040517f08c379a000000000000000000000000000000000000000000000000000000000815260040180806020018281038252603e81526020018061181e603e913960400191505060405180910390fd5b505050505050565b63f23a6e6160e01b7bffffffffffffffffffffffffffffffffffffffffffffffffffffffff19168473ffffffffffffffffffffffffffffffffffffffff1663f23a6e6188888787876040518663ffffffff1660e01b8152600401808673ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1681526020018573ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff16815260200184815260200183815260200180602001828103825283818151815260200191508051906020019080838360005b838110156116c15780820151818401526020810190506116a6565b50505050905090810190601f1680156116ee5780820380516001836020036101000a031916815260200191505b509650505050505050602060405180830381600087803b15801561171157600080fd5b505af1158015611725573d6000803e3d6000fd5b505050506040513d602081101561173b57600080fd5b81019080805190602001909291905050507bffffffffffffffffffffffffffffffffffffffffffffffffffffffff1916146117c1576040517f08c379a00000000000000000000000000000000000000000000000000000000081526004018080602001828103825260398152602001806118856039913960400191505060405180910390fd5b50505050505056fe64657374696e6174696f6e2061646472657373206d757374206265206e6f6e2d7a65726f2e4e656564206f70657261746f7220617070726f76616c20666f7220337264207061727479207472616e73666572732e636f6e74726163742072657475726e656420616e20756e6b6e6f776e2076616c75652066726f6d206f6e45524331313535426174636852656365697665645f69647320616e64205f76616c756573206172726179206c656e677468206d757374206d617463682e636f6e74726163742072657475726e656420616e20756e6b6e6f776e2076616c75652066726f6d206f6e455243313135355265636569766564a265627a7a72315820219bb5de54ccb0c0e170421aa30869ef4c87e77aea9eaeef5ed5e1d6abc0cf5264736f6c63430005100032";

    public static final String FUNC_SUPPORTSINTERFACE = "supportsInterface";

    public static final String FUNC_SAFETRANSFERFROM = "safeTransferFrom";

    public static final String FUNC_SAFEBATCHTRANSFERFROM = "safeBatchTransferFrom";

    public static final String FUNC_BALANCEOF = "balanceOf";

    public static final String FUNC_BALANCEOFBATCH = "balanceOfBatch";

    public static final String FUNC_SETAPPROVALFORALL = "setApprovalForAll";

    public static final String FUNC_ISAPPROVEDFORALL = "isApprovedForAll";

    public static final Event APPROVALFORALL_EVENT = new Event("ApprovalForAll", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Address>(true) {}, new TypeReference<Address>(true) {}, new TypeReference<Bool>() {}));
    ;

    public static final Event TRANSFERBATCH_EVENT = new Event("TransferBatch", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Address>(true) {}, new TypeReference<Address>(true) {}, new TypeReference<Address>(true) {}, new TypeReference<DynamicArray<Uint256>>() {}, new TypeReference<DynamicArray<Uint256>>() {}));
    ;

    public static final Event TRANSFERSINGLE_EVENT = new Event("TransferSingle", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Address>(true) {}, new TypeReference<Address>(true) {}, new TypeReference<Address>(true) {}, new TypeReference<Uint256>() {}, new TypeReference<Uint256>() {}));
    ;

    public static final Event URI_EVENT = new Event("URI", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Utf8String>() {}, new TypeReference<Uint256>(true) {}));
    ;

    protected static final HashMap<String, String> _addresses;

    static {
        _addresses = new HashMap<String, String>();
    }

    @Deprecated
    protected ERC1155(String contractAddress, Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    protected ERC1155(String contractAddress, Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, credentials, contractGasProvider);
    }

    @Deprecated
    protected ERC1155(String contractAddress, Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    protected ERC1155(String contractAddress, Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, transactionManager, contractGasProvider);
    }

    public List<ApprovalForAllEventResponse> getApprovalForAllEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = extractEventParametersWithLog(APPROVALFORALL_EVENT, transactionReceipt);
        ArrayList<ApprovalForAllEventResponse> responses = new ArrayList<ApprovalForAllEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            ApprovalForAllEventResponse typedResponse = new ApprovalForAllEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse._owner = (String) eventValues.getIndexedValues().get(0).getValue();
            typedResponse._operator = (String) eventValues.getIndexedValues().get(1).getValue();
            typedResponse._approved = (Boolean) eventValues.getNonIndexedValues().get(0).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public Flowable<ApprovalForAllEventResponse> approvalForAllEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(new Function<Log, ApprovalForAllEventResponse>() {
            @Override
            public ApprovalForAllEventResponse apply(Log log) {
                Contract.EventValuesWithLog eventValues = extractEventParametersWithLog(APPROVALFORALL_EVENT, log);
                ApprovalForAllEventResponse typedResponse = new ApprovalForAllEventResponse();
                typedResponse.log = log;
                typedResponse._owner = (String) eventValues.getIndexedValues().get(0).getValue();
                typedResponse._operator = (String) eventValues.getIndexedValues().get(1).getValue();
                typedResponse._approved = (Boolean) eventValues.getNonIndexedValues().get(0).getValue();
                return typedResponse;
            }
        });
    }

    public Flowable<ApprovalForAllEventResponse> approvalForAllEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(APPROVALFORALL_EVENT));
        return approvalForAllEventFlowable(filter);
    }

    public List<TransferBatchEventResponse> getTransferBatchEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = extractEventParametersWithLog(TRANSFERBATCH_EVENT, transactionReceipt);
        ArrayList<TransferBatchEventResponse> responses = new ArrayList<TransferBatchEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            TransferBatchEventResponse typedResponse = new TransferBatchEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse._operator = (String) eventValues.getIndexedValues().get(0).getValue();
            typedResponse._from = (String) eventValues.getIndexedValues().get(1).getValue();
            typedResponse._to = (String) eventValues.getIndexedValues().get(2).getValue();
            typedResponse._ids = (List<BigInteger>) eventValues.getNonIndexedValues().get(0).getValue();
            typedResponse._values = (List<BigInteger>) eventValues.getNonIndexedValues().get(1).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public Flowable<TransferBatchEventResponse> transferBatchEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(new Function<Log, TransferBatchEventResponse>() {
            @Override
            public TransferBatchEventResponse apply(Log log) {
                Contract.EventValuesWithLog eventValues = extractEventParametersWithLog(TRANSFERBATCH_EVENT, log);
                TransferBatchEventResponse typedResponse = new TransferBatchEventResponse();
                typedResponse.log = log;
                typedResponse._operator = (String) eventValues.getIndexedValues().get(0).getValue();
                typedResponse._from = (String) eventValues.getIndexedValues().get(1).getValue();
                typedResponse._to = (String) eventValues.getIndexedValues().get(2).getValue();
                typedResponse._ids = (List<BigInteger>) eventValues.getNonIndexedValues().get(0).getValue();
                typedResponse._values = (List<BigInteger>) eventValues.getNonIndexedValues().get(1).getValue();
                return typedResponse;
            }
        });
    }

    public Flowable<TransferBatchEventResponse> transferBatchEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(TRANSFERBATCH_EVENT));
        return transferBatchEventFlowable(filter);
    }

    public List<TransferSingleEventResponse> getTransferSingleEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = extractEventParametersWithLog(TRANSFERSINGLE_EVENT, transactionReceipt);
        ArrayList<TransferSingleEventResponse> responses = new ArrayList<TransferSingleEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            TransferSingleEventResponse typedResponse = new TransferSingleEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse._operator = (String) eventValues.getIndexedValues().get(0).getValue();
            typedResponse._from = (String) eventValues.getIndexedValues().get(1).getValue();
            typedResponse._to = (String) eventValues.getIndexedValues().get(2).getValue();
            typedResponse._id = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
            typedResponse._value = (BigInteger) eventValues.getNonIndexedValues().get(1).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public Flowable<TransferSingleEventResponse> transferSingleEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(new Function<Log, TransferSingleEventResponse>() {
            @Override
            public TransferSingleEventResponse apply(Log log) {
                Contract.EventValuesWithLog eventValues = extractEventParametersWithLog(TRANSFERSINGLE_EVENT, log);
                TransferSingleEventResponse typedResponse = new TransferSingleEventResponse();
                typedResponse.log = log;
                typedResponse._operator = (String) eventValues.getIndexedValues().get(0).getValue();
                typedResponse._from = (String) eventValues.getIndexedValues().get(1).getValue();
                typedResponse._to = (String) eventValues.getIndexedValues().get(2).getValue();
                typedResponse._id = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
                typedResponse._value = (BigInteger) eventValues.getNonIndexedValues().get(1).getValue();
                return typedResponse;
            }
        });
    }

    public Flowable<TransferSingleEventResponse> transferSingleEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(TRANSFERSINGLE_EVENT));
        return transferSingleEventFlowable(filter);
    }

    public List<URIEventResponse> getURIEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = extractEventParametersWithLog(URI_EVENT, transactionReceipt);
        ArrayList<URIEventResponse> responses = new ArrayList<URIEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            URIEventResponse typedResponse = new URIEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse._id = (BigInteger) eventValues.getIndexedValues().get(0).getValue();
            typedResponse._value = (String) eventValues.getNonIndexedValues().get(0).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public Flowable<URIEventResponse> uRIEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(new Function<Log, URIEventResponse>() {
            @Override
            public URIEventResponse apply(Log log) {
                Contract.EventValuesWithLog eventValues = extractEventParametersWithLog(URI_EVENT, log);
                URIEventResponse typedResponse = new URIEventResponse();
                typedResponse.log = log;
                typedResponse._id = (BigInteger) eventValues.getIndexedValues().get(0).getValue();
                typedResponse._value = (String) eventValues.getNonIndexedValues().get(0).getValue();
                return typedResponse;
            }
        });
    }

    public Flowable<URIEventResponse> uRIEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(URI_EVENT));
        return uRIEventFlowable(filter);
    }

    public RemoteFunctionCall<Boolean> supportsInterface(byte[] _interfaceId) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(FUNC_SUPPORTSINTERFACE, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Bytes4(_interfaceId)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Bool>() {}));
        return executeRemoteCallSingleValueReturn(function, Boolean.class);
    }

    public RemoteFunctionCall<TransactionReceipt> safeTransferFrom(String _from, String _to, BigInteger _id, BigInteger _value, byte[] _data) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(
                FUNC_SAFETRANSFERFROM, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(_from), 
                new org.web3j.abi.datatypes.Address(_to), 
                new org.web3j.abi.datatypes.generated.Uint256(_id), 
                new org.web3j.abi.datatypes.generated.Uint256(_value), 
                new org.web3j.abi.datatypes.DynamicBytes(_data)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> safeBatchTransferFrom(String _from, String _to, List<BigInteger> _ids, List<BigInteger> _values, byte[] _data) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(
                FUNC_SAFEBATCHTRANSFERFROM, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(_from), 
                new org.web3j.abi.datatypes.Address(_to), 
                new org.web3j.abi.datatypes.DynamicArray<org.web3j.abi.datatypes.generated.Uint256>(
                        org.web3j.abi.datatypes.generated.Uint256.class,
                        org.web3j.abi.Utils.typeMap(_ids, org.web3j.abi.datatypes.generated.Uint256.class)), 
                new org.web3j.abi.datatypes.DynamicArray<org.web3j.abi.datatypes.generated.Uint256>(
                        org.web3j.abi.datatypes.generated.Uint256.class,
                        org.web3j.abi.Utils.typeMap(_values, org.web3j.abi.datatypes.generated.Uint256.class)), 
                new org.web3j.abi.datatypes.DynamicBytes(_data)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<BigInteger> balanceOf(String _owner, BigInteger _id) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(FUNC_BALANCEOF, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(_owner), 
                new org.web3j.abi.datatypes.generated.Uint256(_id)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}));
        return executeRemoteCallSingleValueReturn(function, BigInteger.class);
    }

    public RemoteFunctionCall<List> balanceOfBatch(List<String> _owners, List<BigInteger> _ids) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(FUNC_BALANCEOFBATCH, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.DynamicArray<org.web3j.abi.datatypes.Address>(
                        org.web3j.abi.datatypes.Address.class,
                        org.web3j.abi.Utils.typeMap(_owners, org.web3j.abi.datatypes.Address.class)), 
                new org.web3j.abi.datatypes.DynamicArray<org.web3j.abi.datatypes.generated.Uint256>(
                        org.web3j.abi.datatypes.generated.Uint256.class,
                        org.web3j.abi.Utils.typeMap(_ids, org.web3j.abi.datatypes.generated.Uint256.class))), 
                Arrays.<TypeReference<?>>asList(new TypeReference<DynamicArray<Uint256>>() {}));
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

    public RemoteFunctionCall<TransactionReceipt> setApprovalForAll(String _operator, Boolean _approved) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(
                FUNC_SETAPPROVALFORALL, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(_operator), 
                new org.web3j.abi.datatypes.Bool(_approved)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<Boolean> isApprovedForAll(String _owner, String _operator) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(FUNC_ISAPPROVEDFORALL, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(_owner), 
                new org.web3j.abi.datatypes.Address(_operator)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Bool>() {}));
        return executeRemoteCallSingleValueReturn(function, Boolean.class);
    }

    @Deprecated
    public static ERC1155 load(String contractAddress, Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        return new ERC1155(contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    @Deprecated
    public static ERC1155 load(String contractAddress, Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        return new ERC1155(contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    public static ERC1155 load(String contractAddress, Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        return new ERC1155(contractAddress, web3j, credentials, contractGasProvider);
    }

    public static ERC1155 load(String contractAddress, Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        return new ERC1155(contractAddress, web3j, transactionManager, contractGasProvider);
    }

    public static RemoteCall<ERC1155> deploy(Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        return deployRemoteCall(ERC1155.class, web3j, credentials, contractGasProvider, BINARY, "");
    }

    @Deprecated
    public static RemoteCall<ERC1155> deploy(Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        return deployRemoteCall(ERC1155.class, web3j, credentials, gasPrice, gasLimit, BINARY, "");
    }

    public static RemoteCall<ERC1155> deploy(Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        return deployRemoteCall(ERC1155.class, web3j, transactionManager, contractGasProvider, BINARY, "");
    }

    @Deprecated
    public static RemoteCall<ERC1155> deploy(Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        return deployRemoteCall(ERC1155.class, web3j, transactionManager, gasPrice, gasLimit, BINARY, "");
    }

    protected String getStaticDeployedAddress(String networkId) {
        return _addresses.get(networkId);
    }

    public static String getPreviouslyDeployedAddress(String networkId) {
        return _addresses.get(networkId);
    }

    public static class ApprovalForAllEventResponse extends BaseEventResponse {
        public String _owner;

        public String _operator;

        public Boolean _approved;
    }

    public static class TransferBatchEventResponse extends BaseEventResponse {
        public String _operator;

        public String _from;

        public String _to;

        public List<BigInteger> _ids;

        public List<BigInteger> _values;
    }

    public static class TransferSingleEventResponse extends BaseEventResponse {
        public String _operator;

        public String _from;

        public String _to;

        public BigInteger _id;

        public BigInteger _value;
    }

    public static class URIEventResponse extends BaseEventResponse {
        public BigInteger _id;

        public String _value;
    }
}
