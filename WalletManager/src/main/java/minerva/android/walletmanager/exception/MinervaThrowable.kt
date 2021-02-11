package minerva.android.walletmanager.exception

class IsNotSafeAccountMasterOwnerThrowable : Throwable()
class BalanceIsNotEmptyAndHasMoreOwnersThrowable : Throwable()
class BalanceIsNotEmptyThrowable : Throwable()
class NotInitializedWalletConfigThrowable : Throwable("Wallet Config was not initialized")
class NoIdentityToRemoveThrowable : Throwable("Missing identity to remove")
class CannotRemoveLastIdentityThrowable : Throwable("You can not remove last identity")
class OwnerAlreadyAddedThrowable : Throwable("Error: Owner already added!")
class CannotRemoveMasterOwnerAddressThrowable : Throwable("Error: Cannot remove masterOwner Address!")
class AlreadyRemovedOwnerThrowable : Throwable("Error: No address owner on the list!")
class MissingAccountThrowable : Throwable("Missing account with this index")
class NotSupportedAccountThrowable : Throwable("Not supported Account type")
class NoActiveNetworkThrowable : Throwable("No active Networks in Config File")
class NoAddressPageFragmentThrowable : Throwable("This fragment type is not supported!")
class NoBindedCredentialThrowable : Throwable("There is no such Credential binded to any Identity")
class NoLoggedInIdentityThrowable : Throwable("The provided did does not match to any existed Identity")
class AllTokenIconsUpdated() : Throwable("All Token Icons are updated")
class AutomaticBackupFailedThrowable : Throwable()
class EncodingJwtFailedThrowable : Throwable()
class InvalidAccountException : Throwable("Invalid Account error")
class NetworkNotFound : Throwable("Network not found!")
