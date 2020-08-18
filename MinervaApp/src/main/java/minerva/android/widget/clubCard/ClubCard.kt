package minerva.android.widget.clubCard

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.MotionEvent
import android.view.Window
import io.reactivex.Observable
import io.reactivex.ObservableOnSubscribe
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.club_card_layout.*
import kotlinx.android.synthetic.main.default_card.*
import minerva.android.R
import minerva.android.extension.fadeIn
import minerva.android.extension.gone
import minerva.android.extension.visible
import minerva.android.extension.visibleOrGone
import minerva.android.kotlinUtils.Empty
import minerva.android.walletmanager.model.mappers.*
import org.jsoup.Jsoup
import org.w3c.dom.Document
import timber.log.Timber
import java.io.StringWriter
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

abstract class ClubCard(context: Context, private val path: String) : Dialog(context, R.style.CardDialog) {

    abstract fun getAsHashMap(): HashMap<String, String>
    private val propertyMap: HashMap<String, String> by lazy {
        getAsHashMap()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prepareWebView()

        val disposable = downloadWebPageSource().firstOrError()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSubscribe { handleLoader(true) }
            .doOnEvent { _, _ -> handleLoader(false) }
            .subscribeBy(
                onSuccess = {
                    correctXML(it)
                },
                onError = {
                    showDefaultCard()
                    Timber.e("Creating card preview error: ${it.message}")
                }
            )

        setOnDismissListener {
            disposable.dispose()
        }
    }

    private fun correctXML(xml: String = String.Empty) {
        prepareDoc(xml)?.let { doc ->
            doc.getElementsByTagName(TEXT).let {
                for (i in 0..it.length) {
                    it.item(i)?.attributes?.let { attributes ->
                        for (j in 0 until attributes.length) {
                            propertyMap[attributes.item(j).nodeValue]?.let { value -> it.item(i)?.textContent = value }
                        }
                    }
                }
            }
            webView.loadDataWithBaseURL(null, toString(doc), MIME_TYPE, UTF8, null)
        }
    }

    private fun prepareDoc(xml: String): Document? =
        try {
            DocumentBuilderFactory
                .newInstance()
                .newDocumentBuilder().parse(xml.byteInputStream())
        } catch (e: Exception) {
            showDefaultCard()
            null
        }

    private fun showDefaultCard() {
        webView.gone()
        defaultCard.visible()

        cardName.text = propertyMap[CREDENTIAL_NAME]
        valid.text = propertyMap[EXP]
        memberId.text = propertyMap[MEMBER_ID]
        memberName.text = propertyMap[NAME]
        since.text = propertyMap[SINCE]
        coverage.text = propertyMap[COVERAGE]
        ok.setOnClickListener { dismiss() }
    }

    private fun toString(doc: Document): String {
        StringWriter().let {
            TransformerFactory.newInstance().newTransformer().apply {
                setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, NO)
                setOutputProperty(OutputKeys.METHOD, XML)
                setOutputProperty(OutputKeys.INDENT, YES)
                setOutputProperty(OutputKeys.ENCODING, UTF8)
                transform(DOMSource(doc), StreamResult(it))
            }
            return it.toString()
        }
    }

    private fun downloadWebPageSource() = Observable.create(ObservableOnSubscribe<String> { observable ->
        Jsoup.connect(path).ignoreContentType(true).get().let {
            it.outerHtml().let { html ->
                observable.onNext(html)
            }
        }
    })

    private fun prepareWebView() {
        webView.apply {
            settings.javaScriptEnabled = true
            setBackgroundColor(Color.TRANSPARENT)
            isVerticalScrollBarEnabled = false
            isHorizontalFadingEdgeEnabled = false
            setOnTouchListener { _, motionEvent -> motionEvent.action == MotionEvent.ACTION_MOVE }
        }
    }

    private fun handleLoader(isVisible: Boolean) {
        progress.visibleOrGone(isVisible)
        webView.visibleOrGone(!isVisible)
        if (!isVisible) webView.fadeIn()
    }

    init {
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setCancelable(true)
        setContentView(R.layout.club_card_layout)
    }

    companion object {
        private const val TEXT = "text"
        private const val YES = "yes"
        private const val NO = "no"
        private const val XML = "xml"
        private const val UTF8 = "UTF-8"
        private const val MIME_TYPE = "text/html"
    }
}

