import minerva.android.walletConnect.model.session.WCSession
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Test
import kotlin.test.assertFailsWith

// todo: fix these tests
class WCSessionTest {
    /*
    @Test
    fun `from should create WCSession from valid v1 uri`() {
        val uri =
            "wc:ac9e9b4d-1eac-4d88-b832-eadc9680c2a4@1?bridge=https%3A%2F%2Fg.bridge.walletconnect.org&key=be3c056d33ad136c11dcb856776d81fb32bf4029ae6d2aaf13148490ec32b559"
        val session = WCSession.from(uri)

        session.topic shouldBeEqualTo "ac9e9b4d-1eac-4d88-b832-eadc9680c2a4"
        session.version shouldBeEqualTo "1"
        session.key shouldBeEqualTo "be3c056d33ad136c11dcb856776d81fb32bf4029ae6d2aaf13148490ec32b559"
        session.bridge shouldBeEqualTo "https://g.bridge.walletconnect.org"
        session.relayProtocol shouldBeEqualTo null
        session.relayData shouldBeEqualTo null
    }

    @Test
    fun `from should create WCSession from valid v2 uri`() {
        val uri =
            "wc:ac9e9b4d-1eac-4d88-b832-eadc9680c2a4@2?symKey=be3c056d33ad136c11dcb856776d81fb32bf4029ae6d2aaf13148490ec32b559&relay-protocol=https%3A%2F%2Fg.relay.walletconnect.org&relay-data=data"
        val session = WCSession.from(uri)

        session.topic shouldBeEqualTo "ac9e9b4d-1eac-4d88-b832-eadc9680c2a4"
        session.version shouldBeEqualTo "2"
        session.key shouldBeEqualTo "be3c056d33ad136c11dcb856776d81fb32bf4029ae6d2aaf13148490ec32b559"
        session.bridge shouldBeEqualTo null
        session.relayProtocol shouldBeEqualTo "https://g.relay.walletconnect.org"
        session.relayData shouldBeEqualTo "data"
    }

    @Test
    fun `WCSession should throw exception when invalid qr code is provided`() {
        val invalidUri = "wc:ac9e9b4d-1eac-4d88-b832-eadc9680c2a4@3?bridge=https%3A%2F%2Fg.bridge.walletconnect.org&key=be3c056d33ad136c11dcb856776d81fb32bf4029ae6d2aaf13148490ec32b559"
        assertFailsWith<Throwable> { WCSession.from(invalidUri) }
    }

    */
}
