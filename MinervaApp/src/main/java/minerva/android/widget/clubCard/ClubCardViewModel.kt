package minerva.android.widget.clubCard

import com.google.gson.annotations.SerializedName
import io.reactivex.Observable
import io.reactivex.ObservableOnSubscribe
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import minerva.android.base.BaseViewModel
import minerva.android.kotlinUtils.DateUtils
import minerva.android.kotlinUtils.function.orElse
import minerva.android.walletmanager.model.Credential
import org.jsoup.Jsoup
import org.w3c.dom.Document
import timber.log.Timber
import java.io.StringWriter
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult
import kotlin.reflect.full.declaredMemberProperties

class ClubCardViewModel(private val cacheStorage: CacheStorage) : BaseViewModel() {

    lateinit var propertyMap: HashMap<String, String>
    lateinit var stateCallback: ClubCardStateCallback

    fun loadCardData(credential: Credential, stateCallback: ClubCardStateCallback) {
        this.stateCallback = stateCallback
        propertyMap = getAsHashMap(credential)
        cacheStorage.load(credential.cardUrl)?.let { correctXML(it) }
            .orElse { downloadCardSource(credential.cardUrl) }
    }

    private fun downloadCardSource(cardUrl: String?) {
        launchDisposable {
            downloadWebPageSource(cardUrl).firstOrError()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe { stateCallback.onLoading(true) }
                .doOnEvent { _, _ -> stateCallback.onLoading(false) }
                .subscribeBy(
                    onSuccess = {
                        correctXML(it)
                    },
                    onError = {
                        Timber.e("Creating card preview error: ${it.message}")
                        stateCallback.onError()
                    }
                )
        }
    }

    private fun correctXML(cardSource: String, url: String? = null){
        prepareDoc(cardSource)?.let { doc ->
            doc.getElementsByTagName(TEXT).let {
                for (i in 0..it.length) {
                    it.item(i)?.attributes?.let { attributes ->
                        for (j in 0 until attributes.length) {
                            propertyMap[attributes.item(j).nodeValue]?.let { value -> it.item(i)?.textContent = value }
                        }
                    }
                }
            }
            url?.let { cacheStorage.save(it, cardSource) }
            stateCallback.onCardDataPrepared(toString(doc))
        }
    }

    private fun prepareDoc(xml: String): Document? =
        try {
            DocumentBuilderFactory
                .newInstance()
                .newDocumentBuilder().parse(xml.byteInputStream())
        } catch (e: Exception) {
            Timber.e("XML Parse error: ${e.message}")
            stateCallback.onError()
            null
        }

    private fun downloadWebPageSource(cardUrl: String?) = Observable.create(ObservableOnSubscribe<String> { observable ->
        Jsoup.connect(cardUrl).ignoreContentType(true).get().let {
            it.outerHtml().let { html ->
                observable.onNext(html)
            }
        }.orElse { observable.onError(Throwable("Missing car URL")) }
    })

    private fun getAsHashMap(credential: Credential): HashMap<String, String> =
        HashMap<String, String>().apply {
            for (prop in Credential::class.declaredMemberProperties) {
                Credential::class.java.getDeclaredField(prop.name).let { field ->
                    field.getAnnotation(SerializedName::class.java)?.let { fieldSerializedName ->
                        this[fieldSerializedName.value] = getProperFormat(prop.get(credential))
                    } ?: run {
                        this[prop.name] = getProperFormat(prop.get(credential))
                    }
                }
            }
        }

    private fun getProperFormat(data: Any?): String = when (data) {
        is Long -> DateUtils.getDateFromTimestamp(data, DateUtils.SHORT_DATE_FORMAT)
        else -> data.toString()
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

    companion object {
        private const val TEXT = "text"
        private const val YES = "yes"
        private const val NO = "no"
        private const val XML = "xml"
        const val UTF8 = "UTF-8"
    }
}