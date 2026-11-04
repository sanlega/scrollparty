package com.santilega.scrollparty.ui.screens

import android.content.ActivityNotFoundException
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.santilega.scrollparty.GameViewModel
import com.santilega.scrollparty.R
import com.santilega.scrollparty.bitmapFromUri
import com.santilega.scrollparty.squareJpegBase64
import com.santilega.scrollparty.ui.components.AppTextField
import com.santilega.scrollparty.ui.components.DisplayTitle
import com.santilega.scrollparty.ui.components.LinkButton
import com.santilega.scrollparty.ui.components.Machine
import com.santilega.scrollparty.ui.components.PlayerFace
import com.santilega.scrollparty.ui.components.PrimaryButton
import com.santilega.scrollparty.ui.components.ScreenScaffold
import com.santilega.scrollparty.ui.theme.SPColor
import com.santilega.scrollparty.ui.theme.SPSpace
import com.santilega.scrollparty.ui.theme.SPType

/** Color con el que se previsualiza la cara; el definitivo se asigna al entrar en un grupo. */
private const val PREVIEW_COLOR = 5

/**
 * Selfie y nombre. Cámara con TakePicturePreview (no necesita permiso de cámara) y galería como alternativa;
 * también se puede seguir solo con iniciales.
 */
@Composable
fun ProfileScreen(vm: GameViewModel) {
    val context = LocalContext.current
    var name by rememberSaveable { mutableStateOf(vm.name) }
    var needName by rememberSaveable { mutableStateOf(false) }
    val photo = vm.photo
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        bitmap?.let { vm.savePhoto(squareJpegBase64(it)) }
    }
    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let {
            val bitmap = bitmapFromUri(context, it)
            if (bitmap != null) vm.savePhoto(squareJpegBase64(bitmap)) else vm.error = context.getString(R.string.photo_failed)
        }
    }
    fun takePhoto() {
        try { camera.launch(null) } catch (_: ActivityNotFoundException) { vm.error = context.getString(R.string.camera_unavailable) }
    }
    fun pickPhoto() = gallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    fun carryOn() {
        if (name.isBlank()) needName = true else vm.saveName(name)
    }

    ScreenScaffold(
        bottom = {
            if (photo == null) {
                PrimaryButton(stringResource(R.string.take_photo), ::takePhoto)
                LinkButton(stringResource(R.string.choose_gallery), ::pickPhoto)
                LinkButton(stringResource(R.string.use_initials), ::carryOn)
            } else {
                PrimaryButton(stringResource(R.string.looks_good), ::carryOn)
                LinkButton(stringResource(R.string.retake), ::takePhoto)
                LinkButton(stringResource(R.string.choose_gallery), ::pickPhoto)
            }
        },
    ) {
        Spacer(Modifier.height(SPSpace.s2))
        DisplayTitle(stringResource(R.string.profile_title), Modifier.fillMaxWidth(), style = SPType.title, align = TextAlign.Center)
        Text(stringResource(R.string.profile_subtitle), Modifier.fillMaxWidth().padding(top = 10.dp, bottom = SPSpace.s3), style = SPType.body, color = SPColor.white, textAlign = TextAlign.Center)
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Machine(Modifier.widthIn(max = 240.dp).fillMaxWidth()) {
                PlayerFace(
                    name, PREVIEW_COLOR, photo,
                    Modifier.fillMaxWidth().aspectRatio(1f).padding(8.dp),
                    contentDescription = stringResource(if (photo == null) R.string.your_initials else R.string.your_photo),
                )
            }
        }
        Spacer(Modifier.height(SPSpace.s3))
        AppTextField(name, { name = it.take(30); needName = false }, stringResource(R.string.your_name), tag = "name")
        if (needName) Text(stringResource(R.string.name_required), Modifier.padding(top = SPSpace.s1), style = SPType.body.copy(fontSize = 16.sp), color = SPColor.yellow)
    }
}
