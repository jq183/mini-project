package com.example.miniproject.UserScreen.ProfileScreen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.miniproject.ui.theme.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset

data class FAQItem(
    val question: String,
    val answer: String,
    val category: String
)

val faqList = listOf(
    FAQItem(
        question = "How do I create a project?",
        answer = "To create a project, tap the '+' button on the My Projects page. Fill in the project details including title, description, category, target amount, and deadline. Make sure all required fields are completed before submitting.",
        category = "Projects"
    ),
    FAQItem(
        question = "How do I donate to a project?",
        answer = "Browse projects on the main page or search for specific projects. Click on a project to view details, then tap the 'Donate' button. Enter your donation amount and confirm the payment.",
        category = "Donations"
    ),
    FAQItem(
        question = "Can I edit my project after creating it?",
        answer = "Yes! Go to My Projects, find your project, tap the three-dot menu, and select 'Edit'. You can update most details except the funding goal if donations have already been received.",
        category = "Projects"
    ),
    FAQItem(
        question = "How do I top up my wallet?",
        answer = "Go to Profile page, tap the '+' button next to your wallet balance. Select your payment method and enter the amount you wish to add. Confirm the transaction to complete the top-up.",
        category = "Payment"
    ),
    FAQItem(
        question = "How do I track my donations?",
        answer = "Go to the 'Backed' tab in My Projects to see all projects you've supported. You can also view detailed transaction history in Profile > Transaction History.",
        category = "Donations"
    ),
    FAQItem(
        question = "Can I cancel my donation?",
        answer = "We sorry to inform that donation cannot be refunded once it is done",
        category = "Donations"
    ),
    FAQItem(
        question = "How do I change my password?",
        answer = "Go to Profile > Change Password. Enter your current password, then enter and confirm your new password. Make sure your new password fulfill the requirement.",
        category = "Account"
    ),
    FAQItem(
        question = "Is my payment information secure?",
        answer = "Yes! We use industry-standard encryption to protect your payment information. We never store your full credit card details on our servers.",
        category = "Payment"
    ),
    FAQItem(
        question = "How do I withdraw funds from my project?",
        answer = "Once your project reaches its funding goal, go to your project details and tap 'Withdraw Funds'. The money will be transferred to your registered bank account within 5-7 business days.",
        category = "Projects"
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FAQPage(navController: NavController) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }

    val categories = listOf("All", "Projects", "Donations", "Payment", "Account")

    val filteredFAQs = faqList.filter { faq ->
        val matchesSearch = faq.question.contains(searchQuery, ignoreCase = true) ||
                faq.answer.contains(searchQuery, ignoreCase = true)
        val matchesCategory = selectedCategory == "All" || faq.category == selectedCategory
        matchesSearch && matchesCategory
    }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier.background(BackgroundWhite)
            ) {
                TopAppBar(
                    title = {
                        Text(
                            text = "FAQ",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryBlue
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Back",
                                tint = PrimaryBlue
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = BackgroundWhite
                    )
                )

                // Search Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(BackgroundWhite)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        textStyle = TextStyle(fontSize = 14.sp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        placeholder = { Text("Search questions") },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = "Search",
                                tint = PrimaryBlue
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(
                                        Icons.Default.Clear,
                                        contentDescription = "Clear",
                                        tint = TextSecondary
                                    )
                                }
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryBlue,
                            unfocusedBorderColor = BorderGray,
                            cursorColor = PrimaryBlue
                        ),
                        singleLine = true
                    )
                }

                ScrollableTabRow(
                    selectedTabIndex = categories.indexOf(selectedCategory),
                    modifier = Modifier.fillMaxWidth(),
                    containerColor = BackgroundWhite,
                    contentColor = PrimaryBlue,
                    edgePadding = 16.dp,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(
                                tabPositions[categories.indexOf(selectedCategory)]
                            ),
                            color = PrimaryBlue,
                            height = 3.dp
                        )
                    },
                    divider = {}
                ) {
                    categories.forEach { category ->
                        Tab(
                            selected = selectedCategory == category,
                            onClick = { selectedCategory = category },
                            modifier = Modifier.clip(RoundedCornerShape(8.dp)),
                            text = {
                                Text(
                                    text = category,
                                    fontSize = 15.sp,
                                    fontWeight = if (selectedCategory == category) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            selectedContentColor = PrimaryBlue,
                            unselectedContentColor = TextSecondary
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundGray)
                .padding(paddingValues)
        ) {
            item { Spacer(modifier = Modifier.height(16.dp)) }

            // FAQ Items
            if (filteredFAQs.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.SearchOff,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = TextSecondary.copy(alpha = 0.5f)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "No results found",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Try adjusting your search or filter",
                                fontSize = 14.sp,
                                color = TextSecondary
                            )
                        }
                    }
                }
            } else {
                items(filteredFAQs) { faq ->
                    FAQItemCard(faq = faq, searchQuery = searchQuery)
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }
}

@Composable
fun FAQItemCard(faq: FAQItem, searchQuery: String) {
    var isExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable { isExpanded = !isExpanded },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = BackgroundWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = faq.category,
                        fontSize = 12.sp,
                        color = PrimaryBlue,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .background(
                                PrimaryBlue.copy(alpha = 0.1f),
                                RoundedCornerShape(6.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    HighlightText(
                        text = faq.question,
                        highlight = searchQuery,
                        normalStyle = TextStyle(
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        ),
                        highlightStyle = TextStyle(
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = BackgroundWhite,
                            background = PrimaryBlue
                        )
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = PrimaryBlue,
                    modifier = Modifier.size(24.dp)
                )
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = BorderGray.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(12.dp))

                    // Highlighted Answer
                    HighlightText(
                        text = faq.answer,
                        highlight = searchQuery,
                        normalStyle = TextStyle(
                            fontSize = 14.sp,
                            color = TextSecondary,
                            lineHeight = 20.sp
                        ),
                        highlightStyle = TextStyle(
                            fontSize = 14.sp,
                            color = TextPrimary,
                            background = WarningOrange.copy(alpha = 0.3f),
                            fontWeight = FontWeight.Bold,
                            lineHeight = 20.sp
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun HighlightText(
    text: String,
    highlight: String,
    normalStyle: TextStyle,
    highlightStyle: TextStyle
) {
    if (highlight.isEmpty()) {
        Text(text = text, style = normalStyle)
        return
    }

    val annotatedString = buildAnnotatedString {
        var currentIndex = 0
        val lowerText = text.lowercase()
        val lowerHighlight = highlight.lowercase()

        while (currentIndex < text.length) {
            val startIndex = lowerText.indexOf(lowerHighlight, currentIndex)

            if (startIndex == -1) {
                append(text.substring(currentIndex))
                break
            }

            if (startIndex > currentIndex) {
                append(text.substring(currentIndex, startIndex))
            }

            val endIndex = startIndex + highlight.length
            pushStyle(
                SpanStyle(
                    color = highlightStyle.color,
                    background = highlightStyle.background,
                    fontWeight = highlightStyle.fontWeight
                )
            )
            append(text.substring(startIndex, endIndex))
            pop()

            currentIndex = endIndex
        }
    }

    Text(text = annotatedString, style = normalStyle)
}