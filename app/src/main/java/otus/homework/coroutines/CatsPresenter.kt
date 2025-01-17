package otus.homework.coroutines

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import java.net.SocketTimeoutException

private const val PRESENTER_CAT_JOB_KEY = "CatJob"

class CatsPresenter(
    private val catFactService: CatFactService,
    private val catImageService: CatImageService,
    private val scope: CoroutineScope = PresenterScope(),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val isDataLoadedCallbaсk: (Boolean) -> Unit
) {

    private var _catsView: ICatsView? = null

    fun onInitComplete() {
        scope.launch {
            val factResponseDeffered = scope.async(ioDispatcher) {
                catFactService.getCatFact()
            }
            val imageResponseDeffered = scope.async(ioDispatcher) {
                catImageService.getCatImage()
            }

            val result = try {
                /** Тестовое пробрасывание  SocketTimeoutException для проверки */
//                    throw SocketTimeoutException()

                val factResponse = factResponseDeffered.await()
                val imageResponse = imageResponseDeffered.await()
                val imageResponseFirstElement = imageResponse.firstOrNull()
                    ?: return@launch

                val cat = mapServerResponseToCat(
                    factResponse = factResponse,
                    imageResponse = imageResponseFirstElement
                )

                Result.Success<Cat>(cat)
            } catch (e: CancellationException) {
                throw e
            } catch (e: SocketTimeoutException) {
                Result.Error.SocketError
            } catch (e: Throwable) {
                CrashMonitor.trackWarning(e)
                Result.Error.OtherError(e)
            }

            /**
             * Логика показа Тоста находится внутри View,
             * работает в зависимости от результата
             */
            _catsView?.populate(result)
            isDataLoadedCallbaсk(true)
        }
    }

    fun attachView(catsView: ICatsView) {
        _catsView = catsView
    }

    fun detachView() {
        _catsView = null
    }
}