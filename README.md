# About

Minerva is a Digital Wallet for Android phones.  
It uses emerging standards for Self Sovereign Identity (SSI) for identity management and supports multiple Blockchain networks for handling of digital assets (currently supported: native tokens, ERC-20 tokens and [Gnosis Safe](https://gnosis-safe.io/) accounts).  
Development was co-sponsored by [Sitra](https://www.sitra.fi/en/) as an [IHAN pilot project](https://www.sitra.fi/en/projects/ihan-proof-concept-pilots/) "Minerva MVP".

## Modules

Minerva App Project is divided into several modules:

MinervaApp - the main application module. All UI etc. This module is connected with WalletManager only.

`WalletManager` - module, which is responsible preparing data which is needed for proper MinarvaApp working. Important part of this module is KeyStore, which is responsible for storing encryptionKey for encoding MasterKey. All rest data (private/public keys, addresses, mnemonic) are calculating on the application start. Another important part of this module is WalletConfigRepository. This part of code is responsible for building the wallet using WalletConfig file.

`WalletConfigRepository` - module which is responsible for managing saving and loading WalletConfig file localy and on the server.

`ExchangeMarketsProvider` - module which is responsible for downloading current cryptocurrency to traditional currency ratio (ETH-EUR).

`BlockchainProvider` - this module is responsible for communication with blockchain. There is communication for simple blockchain operations (BlockchainRepository) and for SmartContracts (SmartContractRepository).

`CryptographyProvider` - module which is responsible for all cryptography calculations - public/private keys, addresses, mnemonic using MasterKey. All this calculations are made with every application start.

`ServiceApiProvider` - ServiceApiProvider was created in order to integrate 3rd party login using deep link mechanism.

`WalletConfigProvider` - module where we make save/load operations of WalletConfig raw file. Data is saved in local storage and on the external server. The application is able to run without internet connection (restore walletConfig from local storage).

`KotlinUtils`, `UiExtension` - helpers modules - some universal code, widgets and extension to create application code more readable.

`WalletConfig` - json file which is uploaded/downloaded form the server (saved localy too). This is kind of map of the account. Using this file, the app is able to recreate user account and data. In this file you can find identities (with data) and wallet structure.

![Diagram Image](MinervaDiagram.jpg)

## Description

Application architecture uses dependency injection - Koin. All modules and viewModels are described in file AppModule.kt. All between modules communication is made using streams - RxKotlin. Communication between viewModels and views are made using LiveData. All screens which need WalletConfig data are observing `walletConfigLiveData`, so there is no need to refresh screens manually, after changing wallet data.

All hacks and pleaces which need refactor are described in the code with `TODO` comments

All Networks are described in Network.kt enum class, ie:

```
ETHEREUM(NetworkFullName.ETH, NetworkShortName.ETH, BuildConfig.ETH_ADDRESS, ETHEREUM_GAS_PRICE)
```

### Adding new Network

1. Add Network as new element in Network enum class
2. Add correct shot (which is id), long name, Network default gas price and Network address (build.gradle file) in the same way, as others Networks.
3. Add missing Network Materials in NetworkMaterialRepository (colors, icons, etc)

##Build
Build is done via docker. If you want to build an app and have docker installed just type:

### Debug
- `docker-compose up`
### Release
- `docker-compose run builder ./gradlew assembleRelease`
