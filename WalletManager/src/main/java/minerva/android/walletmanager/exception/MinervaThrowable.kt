package minerva.android.walletmanager.exception

class IsNotSafeAccountMasterOwnerThrowable : Throwable()
class BalanceIsNotEmptyAndHasMoreOwnersThrowable : Throwable()
class NotInitializedWalletConfigThrowable : Throwable("Wallet Config was not initialized")
class NoIdentityToRemoveThrowable : Throwable("Missing identity to remove")
class CannotRemoveLastIdentityThrowable : Throwable("You can not remove last identity")
class OwnerAlreadyAddedThrowable : Throwable("Error: Owner already added!")
class CannotRemoveMasterOwnerAddressThrowable : Throwable("Error: Cannot remove masterOwner Address!")
class AlreadyRemovedOwnerThrowable : Throwable("Error: No address owner on the list!")
class MissingKeysThrowable : Throwable("Missing calculated keys")
class MissingAccountThrowable : Throwable("Missing account with this index")
class NotSupportedAccountThrowable : Throwable("Not supported Account type")
class NoActiveNetworkThrowable : Throwable("No active Networks in Config File")