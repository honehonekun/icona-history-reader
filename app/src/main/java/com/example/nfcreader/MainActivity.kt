package com.example.nfcreader

import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import com.example.nfcreader.ui.theme.NfcReaderTheme
import kotlin.math.abs

class MainActivity : ComponentActivity() {
    private lateinit var nfcViewModel: NfcViewModel
    private lateinit var pendingIntent: PendingIntent
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        nfcViewModel = ViewModelProvider(this).get(NfcViewModel::class.java)

        //起動時にインテントをチェックし中身があるならその時点で処理　（アプリ終了時用）
        if (intent != null) {
            nfcViewModel.processIntent(intent)
        }

        val intent = Intent(this, javaClass).apply {
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_MUTABLE)


        enableEdgeToEdge()
        setContent {
            NfcApp(nfcViewModel = nfcViewModel)
        }
    }

    override fun onResume() {
        super.onResume()
        nfcViewModel.enableForegroundDispatch(this, pendingIntent)
    }

    override fun onPause() {
        super.onPause()
        nfcViewModel.disableForegroundDispatch(this)
    }

    //待機してインテントが発生したら実行　（アプリ実行時用）
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // ViewModelにデータを渡して解析してもらう
        setIntent(intent)
        nfcViewModel.processIntent(intent)
    }
}

@Composable
fun NfcApp(modifier: Modifier = Modifier, nfcViewModel: NfcViewModel) {
    NfcReaderTheme {
        Scaffold(
            modifier = modifier.fillMaxSize(), topBar = {
                NfcTopAppBar(nfcViewModel = nfcViewModel)
            }
        ) {
            NfcScreen(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(it),
                status = nfcViewModel.status,
                list = nfcViewModel.history
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NfcTopAppBar(modifier: Modifier = Modifier, nfcViewModel: NfcViewModel) {
    TopAppBar(
        title = {
            Column {
                Text("交通系ICリーダー")
                Spacer(Modifier.height(4.dp))
                Text(
                    nfcViewModel.status,
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }, modifier = modifier
    )
}


@Composable
fun NfcScreen(modifier: Modifier = Modifier, status: String, list: List<CardModel>) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {


        OutlinedCard(
            modifier = Modifier.padding(32.dp),
            elevation = CardDefaults.cardElevation(8.dp),
            shape = RoundedCornerShape(20.dp)
        ) {
            Box() {

                Image(
                    painter = painterResource(id = R.drawable.icoca_card_svg),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .padding(8.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .fillMaxWidth()
                        .aspectRatio(1.585F / 1F)
                )
            }
        }

        Text(
            text = if (list.isNotEmpty()) {
                "残高:￥${list[0].credit}"
            } else {
                "残高がここに表示されます\nカードをタッチしてください"
            }, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(16.dp))
        if (list.isEmpty()) {
            Spacer(Modifier.weight(1F))
        }
        HorizontalDivider()
        LazyColumn(modifier = Modifier) {
            itemsIndexed(list) { index, item ->
                if (index == list.lastIndex) return@itemsIndexed
                val inStationText = item.inStation?.let { station -> "$station\n" } ?: "\n"
                val outStationText = item.outStation?.let { station -> "$station\n" } ?: "\n"

                val subCredit = item.credit - list[index + 1].credit

                Card(
                    Modifier.padding(vertical = 8.dp, horizontal = 16.dp),
                    shape = RoundedCornerShape(4.dp),
                    colors = CardDefaults.cardColors()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Column() {
                            Row() {
                                Text(item.transaction)
                                Spacer(Modifier.width(8.dp))
                                Text(text = item.time)
                            }
                            Text(
                                text =
                                    inStationText +
                                            outStationText

                            )
                        }

                        Spacer(Modifier.weight(1F))
                        Text(
                            text = if (subCredit <= 0) {
                                "-￥${abs(subCredit)}"
                            } else {
                                "+￥${subCredit}"
                            },
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (subCredit <= 0) {
                                colorResource(R.color.orange)
                            } else {
                                colorResource(R.color.blue)
                            }
                        )
                    }
                }
            }

        }

    }

}

