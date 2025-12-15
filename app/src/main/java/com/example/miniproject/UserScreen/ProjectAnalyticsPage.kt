package com.example.miniproject.UserScreen

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.miniproject.ui.theme.BackgroundWhite
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.example.miniproject.UserScreen.RevenueEntry
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.style.TextAlign
import com.example.miniproject.ui.theme.PrimaryBlue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectAnalyticsPage(
    navController: NavController,
    projectId: String,
    title: String,
    currentAmount: Float,
    goalAmount: Float,
    backers: Int,
    createdAt: Long
) {
    val percentFunded = ((currentAmount / goalAmount) * 100).toInt()
    val createdDate = remember(createdAt) {
        SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())
            .format(Date(createdAt * 1000))
    }
    var revenueData by remember { mutableStateOf<List<RevenueEntry>>(emptyList()) }

    LaunchedEffect(projectId) {
        fetchTop5Donations(projectId) { result ->
            println("🔥 FIREBASE RESULT SIZE = ${result.size}")
            revenueData = result
        }
    }

    Scaffold(
        containerColor = Color.White,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Project Analytics") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                            tint = PrimaryBlue
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.White
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFF6F6F6))
        ) {

            /* ---------- HEADER IMAGE ---------- */
            /*item {
                Image(
                    painter = painterResource(id = R.drawable.analytics_header), // placeholder
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentScale = ContentScale.Crop
                )
            }*/

            /* ---------- STATS ---------- */
            item {
                Spacer(modifier = Modifier.height(12.dp))
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        value = "RM ${currentAmount.toInt()}",
                        label = "Total Raised",
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        value = "$percentFunded%",
                        label = "% Funded",
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        value = backers.toString(),
                        label = "Total Backers",
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(20.dp))
            }


            /* ---------- CHART ---------- */
            item {
                Card(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Top 5 Donations", fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(12.dp))

                        // 🔵 Placeholder for chart
                        RevenueBarChart(
                            data = revenueData,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(12.dp))
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Biggest Spike:",
                                color = Color.Gray,
                                fontSize = 12.sp
                            )
                            Text(
                                text = "RM ${currentAmount.toInt()}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                        }
                    }
                }
            }

            /* ---------- HISTORY ---------- */
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Project History",
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Project created on",
                            color = Color.Gray,
                            textAlign = TextAlign.Center
                        )

                        Text(
                            text = createdDate,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

@Composable
fun StatCard(
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .height(80.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(4.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text(label, fontSize = 12.sp, color = Color.Gray)
        }
    }
}
@Composable
fun RevenueBarChart(
    data: List<RevenueEntry>,
    modifier: Modifier = Modifier
) {
    if (data.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(120.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("No donations yet", color = Color.Gray)
        }
        return
    }

    val maxAmount = data.maxOf { it.amount }.coerceAtLeast(1f) // avoid division by zero

    val textPaint = android.graphics.Paint().apply {
        color = android.graphics.Color.DKGRAY
        textSize = 28f
        textAlign = android.graphics.Paint.Align.CENTER
        isAntiAlias = true
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(220.dp)
    ) {
        val n = data.size.coerceAtLeast(1)
        val barWidth = size.width / (n * 2).coerceAtLeast(2)
        val spaceBetween = barWidth
        val chartHeight = size.height * 0.75f
        val leftPadding = 16.dp.toPx()

        data.forEachIndexed { index, entry ->
            val barHeight = (entry.amount / maxAmount) * chartHeight
            val xCenter = leftPadding + index * (barWidth + spaceBetween) + barWidth / 2
            val yTop = chartHeight - barHeight

            drawRoundRect(
                color = Color(0xFF4F7FFF),
                topLeft = Offset(xCenter - barWidth / 2, yTop),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(12f, 12f)
            )

            drawContext.canvas.nativeCanvas.drawText(
                entry.label,
                xCenter,
                size.height - 12f,
                textPaint
            )
        }
    }
}

private fun fetchTop5Donations(
    projectId: String,
    onResult: (List<RevenueEntry>) -> Unit
) {
    FirebaseFirestore.getInstance()
        .collection("donations")
        .whereEqualTo("projectId", projectId)
        .orderBy("amount", Query.Direction.DESCENDING)
        .limit(5)
        .get()
        .addOnSuccessListener { snapshot ->
            val result = snapshot.documents.mapIndexed { index, doc ->
                RevenueEntry(
                    label = "#${index + 1}",
                    amount = doc.getDouble("amount")?.toFloat() ?: 0f
                )
            }
            onResult(result)
        }
}
