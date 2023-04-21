# Minerva Wallet

Minerva Wallet is a community-governed mobile EVM Android wallet designed for the next generation of real-world applications connected to the thriving DeFi world. The wallet supports multiple blockchain networks, including native tokens, ERC-20 tokens, and NFTs. Think of it as a digital version of your regular wallet, able to manage all the things you have in it, but in a digital, tamper-proof, and highly secure format.

Created in 2019, Minerva gives users their sovereignty back and allows them to join the blockchain revolution by eliminating middlemen like banks and exchanges, identity providers, and data aggregators while assuring privacy-by-design.

Visit us at https://minerva.digital or Twitter @MinervaWallet to learn more.

## Supported Networks

Minerva can be used to interact with Ethereum, Gnosis Chain (former xDai Chain), Polygon, Optimism, Arbitrum, Avalanche, BNB Chain, and Celo, as well as many test networks. You can manage all coins and tokens on each of these networks.

## Features

Minerva is packed with features that make it easy to manage your digital assets, interact with DeFi applications, and more. Some of the key features of Minerva include:

- Interact with DeFi and DApps via WalletConnect integration
- Move coins and tokens between different networks with ease
- Secure management of digital assets on multiple blockchain networks
- Create unique Decentralized Identifiers (DIDs) and receive credentials for them
- Easy backup and restoration of wallet data
- Open-source wallet code for public auditability and security
- Non-extractive and transparent fee collection through the Minerva DAO
- Community-governed via Snapshot when it comes to features and revenue generation
- NFT support for all main networks (ERC-721 & ERC-1155) and support for all NFT media types
- Automated token discovery (e.g. LP tokens)

## Roadmap

You can find all the feature suggestions on our [website](https://minerva.digital). Some of the upcoming highlights are:

- Supporting the latest chains, e.g. Polygon zkEVM or zkSync Era
- Providing WalletConnect v2 support and boosting DApp interaction
- Allowing up to 99 accounts for every supported chain
- Adding watch accounts so that they can be easily monitored
- Enabling tokens Swap and Bridge between all supported EVM chains via Li.Fi


## How We Are Different

Wallets are the gateway to Web3, and it is essential to have open-source, non-extractive, community-governed wallets available for everyone. Minerva Wallet has been such a wallet for years now and intends to be always at the forefront of new features, e.g. Superfluid support, combined with simple usability and a quick setup.

We give the power to our users via three core principles:

- Open-source our wallet code for public auditability and security
- Non-extractive and transparent fee collection through the Minerva DAO
- Community-governed via Snapshot when it comes to features and revenue generation

The $MIVA Super Token plays a central role in this, and as we have been diving through the crypto winter, we are now much more prepared to start significant distributions.

Because Minerva Wallet is a mobile EVM Android wallet, we focus on enabling booming emerging P2P markets (e.g. Nigeria) to use a mobile-only approach for making money and managing crypto on the go. With the derived account structure, the wallet is fully compatible with Metamask or Rabby, which helps to tap also into the desktop world, without a compromise to the usability.


## Modules

Minerva Wallet consists of several modules:

- `MinervaApp`: The main application module that contains the user interface and connects to the WalletManager.
- `WalletManager`: Manages data preparation required for the MinervaApp to function properly, including the KeyStore, which stores the encryption key for encoding the MasterKey.
- `ApiProvider`: Interaction with external APIs.
- `WalletConnect`: WalletConnect 1.0 and 2.0 support.
- `BlockchainProvider`: Facilitates communication with the blockchain for simple operations and smart contracts.
- `CryptographyProvider`: Performs all cryptographic calculations, such as public/private keys, addresses, and mnemonics, using the MasterKey.
- `WalletConfigProvider`: Handles save/load operations for the raw WalletConfig file, both in local storage and on an external server.
- `KotlinUtils` and `UiExtension`: Helper modules containing universal code, widgets, and extensions to improve code readability.

## Architecture

The application architecture utilizes dependency injection through Koin. Modules and view models are defined in the AppModule.kt file. Communication between modules is performed using streams via RxKotlin, and communication between view models and views uses LiveData. Screens that require WalletConfig data observe `walletConfigLiveData`, eliminating the need for manual screen refreshing after changing wallet data.

## Building the App

The app is built using Docker. To build the app with Docker installed, use the following commands:

### Debug

- `docker-compose up`

### Release

- `docker-compose run builder ./gradlew assembleRelease`
