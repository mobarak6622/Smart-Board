package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.engine.CanvasRenderEngine
import com.example.localization.StudioStrings
import com.example.model.BoardPage
import com.example.ui.theme.ElectricIndigo
import com.example.ui.theme.PureWhite
import com.example.ui.theme.StudioSlate200
import com.example.ui.theme.StudioSlate900

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PageManagerSheet(
    pages: List<BoardPage>,
    currentPageIndex: Int,
    strings: StudioStrings,
    onSelectPage: (Int) -> Unit,
    onAddPage: () -> Unit,
    onDuplicatePage: (Int) -> Unit,
    onDeletePage: (Int) -> Unit,
    onExportAllPdf: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = PureWhite
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${strings.pageManagerTitle} (${pages.size})",
                    style = MaterialTheme.typography.titleLarge
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = strings.cancelBtn)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onAddPage,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("sheet_add_page_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(strings.addPage)
                }

                OutlinedButton(
                    onClick = {
                        onExportAllPdf()
                        onDismiss()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("sheet_export_pdf_button")
                ) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(strings.exportAllPdf)
                }
            }

            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 148.dp),
                contentPadding = PaddingValues(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(360.dp)
            ) {
                itemsIndexed(pages, key = { _, p -> p.id }) { index, page ->
                    val isCurrent = index == currentPageIndex
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFFF8FAFC),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                width = if (isCurrent) 3.dp else 1.dp,
                                color = if (isCurrent) ElectricIndigo else StudioSlate200,
                                shape = RoundedCornerShape(16.dp)
                            )
                            .clickable {
                                onSelectPage(index)
                                onDismiss()
                            }
                            .testTag("page_thumbnail_$index")
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            // Live Miniature Thumbnail Canvas
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(9f / 12f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(PureWhite)
                                    .border(1.dp, StudioSlate200, RoundedCornerShape(10.dp))
                            ) {
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    val nativeCanvas = drawContext.canvas.nativeCanvas
                                    nativeCanvas.save()
                                    val scaleX = size.width / 1080f
                                    val scaleY = size.height / 1600f
                                    val uniform = minOf(scaleX, scaleY)
                                    nativeCanvas.scale(uniform, uniform)
                                    CanvasRenderEngine.renderPageToAndroidCanvas(
                                        canvas = nativeCanvas,
                                        width = 1080f,
                                        height = 1600f,
                                        page = page
                                    )
                                    nativeCanvas.restore()
                                }

                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopStart)
                                        .padding(6.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isCurrent) ElectricIndigo else StudioSlate900)
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "${strings.pageLabel} ${index + 1}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = PureWhite,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = { onDuplicatePage(index) },
                                    modifier = Modifier
                                        .size(34.dp)
                                        .testTag("duplicate_page_$index")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = strings.duplicatePage,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                IconButton(
                                    onClick = { onDeletePage(index) },
                                    modifier = Modifier
                                        .size(34.dp)
                                        .testTag("delete_page_$index")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteOutline,
                                        contentDescription = strings.deletePage,
                                        tint = Color(0xFFDC2626),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
