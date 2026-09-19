package com.example.c001apk.ui.article

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.c001apk.logic.model.OSSUploadPrepareResponse
import com.example.c001apk.logic.repository.NetworkRepo
import com.example.c001apk.util.Event
import com.google.gson.Gson
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ArticlePublishViewModel @Inject constructor(
    private val networkRepo: NetworkRepo
) : ViewModel() {

    val toastText = MutableLiveData<Event<String?>>()
    val over = MutableLiveData<Event<Boolean>>()
    val uploadImage = MutableLiveData<Event<OSSUploadPrepareResponse.Data>>()

    /** createFeed 的完整表单数据，发布前由 Activity 组装 */
    var feedData = HashMap<String, String>()

    private val ossUploadPrepareData: HashMap<String, String> = HashMap()
    fun onPostOSSUploadPrepare(uploadFileList: List<ArticleUploadFile>) {
        ossUploadPrepareData["uploadBucket"] = "image"
        ossUploadPrepareData["uploadDir"] = "feed"
        ossUploadPrepareData["is_anonymous"] = "0"
        ossUploadPrepareData["uploadFileList"] = Gson().toJson(uploadFileList)
        ossUploadPrepareData["toUid"] = ""
        ossUploadPrepareData["feed_type"] = "feed"

        viewModelScope.launch(Dispatchers.IO) {
            networkRepo.postOSSUploadPrepare(ossUploadPrepareData)
                .collect { result ->
                    val data = result.getOrNull()
                    if (data != null) {
                        if (data.message != null) {
                            toastText.postValue(Event("uploadPrepare error: ${data.message}"))
                        } else if (data.data != null) {
                            uploadImage.postValue(Event(data.data))
                        }
                    } else {
                        toastText.postValue(Event("response is null"))
                    }
                }
        }
    }

    fun onPostCreateFeed() {
        viewModelScope.launch(Dispatchers.IO) {
            networkRepo.postCreateFeed(feedData)
                .collect { result ->
                    val response = result.getOrNull()
                    if (response != null) {
                        if (response.data?.id != null) {
                            over.postValue(Event(true))
                        } else {
                            response.message?.let {
                                toastText.postValue(Event(it))
                            }
                        }
                    } else {
                        toastText.postValue(Event("response is null"))
                    }
                }
        }
    }
}
