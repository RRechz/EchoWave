package com.samyak.simpletube.ui.screens.settings

import android.os.Build
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.Devices
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.samyak.simpletube.BuildConfig
import com.samyak.simpletube.LocalPlayerAwareWindowInsets
import com.samyak.simpletube.R
import com.samyak.simpletube.ui.component.IconButton
import com.samyak.simpletube.ui.screens.CardItem
import com.samyak.simpletube.ui.utils.backToMain

@Composable
fun shimmerEffect(): Brush {
    val shimmerColors = listOf(
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
    )

    val transition = rememberInfiniteTransition(label = "shimmerEffect")
    val translateAnim = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "shimmerEffect"
    )

    return Brush.linearGradient(
        colors = shimmerColors,
        start = Offset.Zero,
        end = Offset(x = translateAnim.value, y = translateAnim.value)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    val uriHandler = LocalUriHandler.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom))
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.windowInsetsPadding(LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Top)))
        Spacer(Modifier.height(4.dp))

        // Image with shimmer effect
        Box(
            modifier = Modifier
                .size(90.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceColorAtElevation(NavigationBarDefaults.Elevation))
        ) {
            Image(
                painter = painterResource(R.drawable.small_about_echowave),
                contentDescription = null,
                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onBackground, BlendMode.SrcIn),
                modifier = Modifier
                    .matchParentSize()
                    .clickable { }
            )

            // Shimmer effect overlay only for the image
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        brush = shimmerEffect(),
                        shape = RoundedCornerShape(8.dp)
                    )
            )
        }

        // Uygulama adı ve versiyon bilgisi (Örnek dosyadaki gibi)
        // Build Type İnfo for Users
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.Build,
                contentDescription = stringResource(R.string.build_type),
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = stringResource(R.string.build_type))
            Spacer(modifier = Modifier.width(4.dp))
            Text(text = BuildConfig.BUILD_TYPE)
        }

        // Version İnfo for Users
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically

        ){
            Icon(
                imageVector = Icons.Rounded.Verified,
                contentDescription = stringResource(R.string.version),
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = stringResource(R.string.version))
            Spacer(modifier = Modifier.width(4.dp))
            Text(text = BuildConfig.VERSION_NAME)
        }

        // Build Official Check
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically

        ){
            Icon(
                imageVector = Icons.Rounded.VerifiedUser,
                contentDescription = stringResource(R.string.official_build),
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = stringResource(R.string.official_build))
            Spacer(modifier = Modifier.width(4.dp))
            Text(text = stringResource(R.string.official))
        }

        // Device İnfo for Users
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically

        ){
            Icon(
                imageVector = Icons.Rounded.Devices,
                contentDescription = stringResource(R.string.device),
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = stringResource(R.string.device))
            Spacer(modifier = Modifier.width(4.dp))
            Text(text = "${Build.BRAND} ${Build.DEVICE} (${Build.MODEL})")
        }

        // Developer Card
        Spacer(Modifier.height(5.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.person),
                contentDescription = null
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.developer_about),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        }
        UserCards(uriHandler) // Developer Card Call
        Spacer(Modifier.height(5.dp))

        // Links and Buttons
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.links),
                contentDescription = null
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.links_about),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(Modifier.height(5.dp))

        CardItem(
            icon = R.drawable.telegram,
            title = stringResource(R.string.my_channel),
            subtitle = stringResource(R.string.my_channel_info),
            onClick = { uriHandler.openUri("https://t.me/by_BabelSoftware") }
        )

        Spacer(Modifier.height(20.dp))

        CardItem(
            icon = R.drawable.web_site,
            title = stringResource(R.string.web_site),
            subtitle = stringResource(R.string.web_site_info),
            onClick = { uriHandler.openUri("https://helelelerescci.github.io/EchoWEB/") }
        )

        Spacer(Modifier.height(20.dp))

        CardItem(
            icon = R.drawable.babel_software_apps,
            title = stringResource(R.string.other_apps),
            subtitle = stringResource(R.string.other_apps_info),
            onClick = { uriHandler.openUri("https://github.com/RRechz?tab=repositories") }
        )

        Spacer(Modifier.height(20.dp))

        CardItem(
            icon = R.drawable.donate,
            title = stringResource(R.string.donate),
            subtitle = stringResource(R.string.donate_info),
            onClick = { uriHandler.openUri("https://buymeacoffee.com/section") }
        )

        // debug info
        if (BuildConfig.DEBUG) {
            Spacer(Modifier.height(400.dp))
            Row(
                verticalAlignment = Alignment.Top,
            ) {
                Text(
                    text = "Device info (Debug)",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.Red,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                )
            }

            val info = listOf<String>(
                "Device: ${Build.BRAND} ${Build.DEVICE} (${Build.MODEL})",
                "Manufacturer: ${Build.MANUFACTURER}",
                "HW: ${Build.BOARD} (${Build.HARDWARE})",
                "ABIs: ${Build.SUPPORTED_ABIS.joinToString()})",
                "Android: ${Build.VERSION.SDK_INT} (${Build.ID})",
                Build.DISPLAY,
                Build.PRODUCT,
                Build.FINGERPRINT,
                Build.VERSION.SECURITY_PATCH

//                needs sdk 29 or 31
//                Build.SKU ,
//                Build.ODM_SKU ,
//                Build.SOC_MODEL ,
//                Build.SOC_MANUFACTURER ,
            )

            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                info.forEach {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Red,
                    )
                }
            }
        }

    }

    TopAppBar(
        title = { Text(stringResource(R.string.about)) },
        navigationIcon = {
            IconButton(
                onClick = navController::navigateUp,
                onLongClick = navController::backToMain
            ) {
                Icon(
                    Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = null
                )
            }
        },
        scrollBehavior = scrollBehavior
    )
}

@Composable
fun UserCards(uriHandler: UriHandler) {
    Column {
        UserCard(
            imageUrl = "https://avatars.githubusercontent.com/u/178022701?v=4",
            name = "Mustafa Burak Özcan",
            role = stringResource(R.string.info_dev),
            onClick = { uriHandler.openUri("https://github.com/RRechz") }
        )
        UserCard(
            imageUrl = "https://avatars.githubusercontent.com/u/196631623?v=4",
            name = "Rescci",
            role = stringResource(R.string.info_dev2),
            onClick = { uriHandler.openUri("https://github.com/RRechz") }
        )
    }
}

@Composable
fun UserCard(
    imageUrl: String,
    name: String,
    role: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp)
            .height(140.dp)
            .shadow(8.dp, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = rememberAsyncImagePainter(model = imageUrl),
                    contentDescription = null,
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .border(1.dp, MaterialTheme.colorScheme.primary, CircleShape),
                    contentScale = ContentScale.Crop
                )

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        text = name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = role,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }

            // Decorative element
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(40.dp)
                    .offset(x = 20.dp, y = (-20).dp)
                    .background(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f),
                        CircleShape
                    )
            )
        }
    }
}