package com.fluidmatterdevelopment.top10downloaderapp

import android.content.Context
import android.os.AsyncTask
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.PersistableBundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.ListView
import kotlinx.android.synthetic.main.activity_main.*
import java.net.URL
import kotlin.properties.Delegates

class FeedEntry {
    var name = ""
    var artist = ""
    var releaseDate = ""
    var summary = ""
    var imageUrl = ""
    override fun toString(): String {
        return """
            name = $name
            artist = $artist
            releaseDate = $releaseDate
            imageUrl = $imageUrl
        """.trimIndent()
    }
}

class MainActivity : AppCompatActivity() {

    val TAG = "MainActivity"
    private var downLoadData: DownloadData? = null
    private var feedUrl: String = "http://ax.itunes.apple.com/WebObjects/MZStoreServices.woa/ws/RSS/topfreeapplications/limit=%d/xml"
    var feedLimit = 10

    private var feedCashedUrl = "INVALIDATED"
    private val STATE_URL = "feedUrl"
    private val STATE_LIMIT = "feedLimit"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (savedInstanceState != null){
            this.feedUrl = savedInstanceState.getString(STATE_URL)!!
            feedLimit = savedInstanceState.getInt(STATE_LIMIT)
        }

        downLoadUrl(feedUrl.format(feedLimit))
        Log.d(TAG, "onCreate: Done!! :)")

    }

    private fun downLoadUrl(feedUrl: String) {
        if (feedUrl != feedCashedUrl){
            Log.d(TAG, "downLoadUrl: starting AsyncTask")
            downLoadData = DownloadData(this, xmlListView)
            downLoadData?.execute(feedUrl)
            feedCashedUrl = feedUrl
            Log.d(TAG, "downLoadUrl: Done!! :)")
        } else {
            Log.d(TAG,"downLoadUrl - Url Not Changed")
        }

    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.feeds_menu, menu)
        if (feedLimit == 10){
            menu?.findItem(R.id.mnu10)?.isChecked = true
        } else {
            menu?.findItem(R.id.mnu25)?.isChecked = true
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        when (item.itemId) {
            R.id.mnuFree ->
                feedUrl = "http://ax.itunes.apple.com/WebObjects/MZStoreServices.woa/ws/RSS/topfreeapplications/limit=%d/xml"
            R.id.mnuPaid ->
                feedUrl ="http://ax.itunes.apple.com/WebObjects/MZStoreServices.woa/ws/RSS/toppaidapplications/limit=%d/xml"
            R.id.mnuSongs ->
                feedUrl = "http://ax.itunes.apple.com/WebObjects/MZStoreServices.woa/ws/RSS/topsongs/limit=%d/xml"
            R.id.mnu10, R.id.mnu25 -> {
                if (!item.isChecked){
                    item.isChecked = true
                    feedLimit = 35 - feedLimit
                    Log.d(TAG,"onOptionItemSelected: ${item.title} setting feedLimit to $feedLimit")
                } else {
                    Log.d(TAG,"onOptionsItemSelected: ${item.title} setting feedingLimit unchanged")
                }
            }
            R.id.mnuRefresh -> feedCashedUrl = "INVALIDATED"
            else ->
                return super.onOptionsItemSelected(item)
        }
        downLoadUrl(feedUrl.format(feedLimit))
        return true
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(STATE_URL, feedUrl)
        outState.putInt(STATE_LIMIT, feedLimit)
    }

    override fun onDestroy() {
        Log.d(TAG,"Saving downLoadedData")
        super.onDestroy()
        downLoadData?.cancel(true)
    }

    companion object {
        private class DownloadData(context: Context, listView: ListView) :
            AsyncTask<String, Void, String>() {

            val TAG = "DownLoadData"
            var parentContext: Context by Delegates.notNull()
            var parentListView: ListView by Delegates.notNull()

            init {
                parentContext = context
                parentListView = listView
            }

            override fun onPostExecute(result: String) {
                super.onPostExecute(result)
                val parseApplication = ParseApplications()
                parseApplication.parse(result)

                val feedAdapter =
                    FeedAdapter(parentContext, R.layout.list_record, parseApplication.applications)
                parentListView.adapter = feedAdapter
            }

            override fun doInBackground(vararg url: String?): String {
                val rssFeed = downloadXML(url[0])
                if (rssFeed.isEmpty()) {
                    Log.e(TAG,"Error DownLoading!")
                }
                return rssFeed
            }

            private fun downloadXML(urlPath: String?): String {
                return URL(urlPath).readText()
            }
        }
    }
}