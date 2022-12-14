package minerva.android.widget.clubCard

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.MotionEvent
import android.view.Window
import com.bumptech.glide.Glide
import com.google.zxing.EncodeHintType
import kotlinx.android.synthetic.main.club_card_layout.*
import kotlinx.android.synthetic.main.default_card.*
import minerva.android.R
import minerva.android.extension.fadeIn
import minerva.android.extension.visibleOrGone
import minerva.android.walletmanager.model.minervaprimitives.credential.Credential
import minerva.android.walletmanager.model.mappers.*
import net.glxn.qrgen.android.QRCode
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class ClubCard(context: Context, private val credential: Credential) : Dialog(context, R.style.DataDialog), KoinComponent,
    ClubCardStateCallback {

    private val viewModel: ClubCardViewModel by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prepareQrCode()
        prepareWebView()
        viewSwitcher.setOnClickListener { viewSwitcher.showNext() }

        viewModel.loadCardData(credential, this)
    }

    override fun onCardDataPrepared(data: String) {
        webView.apply {
            fadeIn()
            loadDataWithBaseURL(null, data, MIME_TYPE, ClubCardViewModel.UTF8, null)
        }
    }

    override fun onLoading(loading: Boolean) = progress.visibleOrGone(loading)

    override fun onError() = showDefaultCard()

    private fun showDefaultCard() {
        defaultCard.fadeIn()
        viewModel.propertyMap.apply {
            cardName.text = this[CREDENTIAL_NAME]
            valid.text = this[EXP]
            memberId.text = this[MEMBER_ID]
            memberName.text = this[NAME]
            since.text = this[SINCE]
            coverage.text = this[COVERAGE]
        }
    }

    private fun prepareQrCode() {
        context.resources.getDimensionPixelSize(R.dimen.qr_code_size).let { qrCodeSize ->
            QRCode.from(credential.token).withSize(qrCodeSize, qrCodeSize)
                .withHint(EncodeHintType.MARGIN, QR_MARGIN)
                .file()
                .let { qr -> Glide.with(context).load(qr).into(qr_code) }
        }
    }

    private fun prepareWebView() {
        webView.apply {
            settings.javaScriptEnabled = true
            setBackgroundColor(Color.TRANSPARENT)
            isVerticalScrollBarEnabled = false
            isHorizontalFadingEdgeEnabled = false
            setOnTouchListener { _, motionEvent ->
                when (motionEvent.action) {
                    MotionEvent.ACTION_UP -> viewSwitcher.showNext()
                }
                motionEvent.action == MotionEvent.ACTION_MOVE
            }
        }
    }

    init {
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setCancelable(true)
        setContentView(R.layout.club_card_layout)
    }

    companion object {
        private const val QR_MARGIN = 1
        private const val MIME_TYPE = "text/html"
    }
}

