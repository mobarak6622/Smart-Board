package com.example.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.example.model.BackgroundType
import com.example.model.BoardPage
import com.example.model.CanvasObject
import com.example.model.CanvasObjectType
import com.example.model.CanvasStroke
import com.example.model.DrawingToolType
import com.example.model.MathFormulaType
import com.example.model.PointFData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class WhiteboardRepository(
    private val projectDao: ProjectDao,
    private val appContext: Context
) {
    val allProjects: Flow<List<WhiteboardProjectEntity>> = projectDao.getAllProjects()

    suspend fun getProjectById(id: String): WhiteboardProjectEntity? =
        projectDao.getProjectById(id)

    suspend fun saveProject(
        id: String,
        title: String,
        pages: List<BoardPage>
    ) = withContext(Dispatchers.IO) {
        val json = serializePages(pages)
        val entity = WhiteboardProjectEntity(
            id = id,
            title = title,
            updatedAt = System.currentTimeMillis(),
            pageCount = pages.size,
            pagesJson = json
        )
        projectDao.upsertProject(entity)
    }

    suspend fun deleteProject(id: String) = withContext(Dispatchers.IO) {
        projectDao.deleteProjectById(id)
    }

    suspend fun copyUriToLocalImage(uri: Uri): String? = withContext(Dispatchers.IO) {
        try {
            val imagesDir = File(appContext.filesDir, "images").apply { mkdirs() }
            val destFile = File(imagesDir, "img_${UUID.randomUUID()}.png")
            appContext.contentResolver.openInputStream(uri)?.use { input ->
                val original = BitmapFactory.decodeStream(input) ?: return@withContext null
                // Scale down if extraordinarily large to keep budget phones fast
                val maxDim = 1280
                val scale = minOf(
                    1f,
                    maxDim.toFloat() / maxOf(original.width, original.height).coerceAtLeast(1)
                )
                val finalBitmap = if (scale < 1f) {
                    Bitmap.createScaledBitmap(
                        original,
                        (original.width * scale).toInt().coerceAtLeast(1),
                        (original.height * scale).toInt().coerceAtLeast(1),
                        true
                    )
                } else {
                    original
                }
                FileOutputStream(destFile).use { out ->
                    finalBitmap.compress(Bitmap.CompressFormat.PNG, 92, out)
                }
            }
            destFile.absolutePath
        } catch (e: Exception) {
            null
        }
    }

    fun loadStudioPreferences(): com.example.model.StudioPreferences {
        val prefs = appContext.getSharedPreferences("smartboard_studio_prefs", Context.MODE_PRIVATE)
        return com.example.model.StudioPreferences(
            enableTwoFingerZoomPan = prefs.getBoolean("enableTwoFingerZoomPan", true),
            showHandPanTool = prefs.getBoolean("showHandPanTool", true),
            showBrushesTab = prefs.getBoolean("showBrushesTab", true),
            showEraserTab = prefs.getBoolean("showEraserTab", true),
            showShapesTab = prefs.getBoolean("showShapesTab", true),
            showGeometryTab = prefs.getBoolean("showGeometryTab", true),
            showMathTab = prefs.getBoolean("showMathTab", true),
            showInsertTab = prefs.getBoolean("showInsertTab", true),
            showBackgroundTab = prefs.getBoolean("showBackgroundTab", true)
        )
    }

    fun saveStudioPreferences(studioPrefs: com.example.model.StudioPreferences) {
        val prefs = appContext.getSharedPreferences("smartboard_studio_prefs", Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean("enableTwoFingerZoomPan", studioPrefs.enableTwoFingerZoomPan)
            .putBoolean("showHandPanTool", studioPrefs.showHandPanTool)
            .putBoolean("showBrushesTab", studioPrefs.showBrushesTab)
            .putBoolean("showEraserTab", studioPrefs.showEraserTab)
            .putBoolean("showShapesTab", studioPrefs.showShapesTab)
            .putBoolean("showGeometryTab", studioPrefs.showGeometryTab)
            .putBoolean("showMathTab", studioPrefs.showMathTab)
            .putBoolean("showInsertTab", studioPrefs.showInsertTab)
            .putBoolean("showBackgroundTab", studioPrefs.showBackgroundTab)
            .apply()
    }

    companion object {
        fun serializePages(pages: List<BoardPage>): String {
            val pagesArray = JSONArray()
            for (page in pages) {
                val pageObj = JSONObject()
                pageObj.put("id", page.id)
                pageObj.put("backgroundType", page.backgroundType.name)

                val strokesArray = JSONArray()
                for (stroke in page.strokes) {
                    val sObj = JSONObject()
                    sObj.put("id", stroke.id)
                    sObj.put("toolType", stroke.toolType.name)
                    sObj.put("colorArgb", stroke.colorArgb)
                    sObj.put("strokeWidth", stroke.strokeWidth.toDouble())
                    sObj.put("alpha", stroke.alpha.toDouble())
                    sObj.put("polygonSides", stroke.polygonSides)

                    val ptsArray = JSONArray()
                    for (pt in stroke.points) {
                        val ptObj = JSONObject()
                        ptObj.put("x", pt.x.toDouble())
                        ptObj.put("y", pt.y.toDouble())
                        ptObj.put("p", pt.pressure.toDouble())
                        ptsArray.put(ptObj)
                    }
                    sObj.put("points", ptsArray)
                    strokesArray.put(sObj)
                }
                pageObj.put("strokes", strokesArray)

                val objectsArray = JSONArray()
                for (item in page.objects) {
                    val oObj = JSONObject()
                    oObj.put("id", item.id)
                    oObj.put("type", item.type.name)
                    oObj.put("x", item.x.toDouble())
                    oObj.put("y", item.y.toDouble())
                    oObj.put("scale", item.scale.toDouble())
                    oObj.put("rotation", item.rotation.toDouble())
                    oObj.put("colorArgb", item.colorArgb)
                    oObj.put("text", item.text)
                    oObj.put("mathType", item.mathType.name)
                    oObj.put("primaryArg", item.primaryArg)
                    oObj.put("secondaryArg", item.secondaryArg)
                    oObj.put("tertiaryArg", item.tertiaryArg)
                    oObj.put("imagePath", item.imagePath)
                    oObj.put("isBakedToCanvas", item.isBakedToCanvas)
                    objectsArray.put(oObj)
                }
                pageObj.put("objects", objectsArray)
                pagesArray.put(pageObj)
            }
            return pagesArray.toString()
        }

        fun deserializePages(json: String): List<BoardPage> {
            if (json.isBlank()) return listOf(BoardPage())
            return try {
                val pagesArray = JSONArray(json)
                val result = mutableListOf<BoardPage>()
                for (i in 0 until pagesArray.length()) {
                    val pageObj = pagesArray.getJSONObject(i)
                    val bgName = pageObj.optString("backgroundType", BackgroundType.PLAIN_WHITE.name)
                    val bgType = runCatching { BackgroundType.valueOf(bgName) }
                        .getOrDefault(BackgroundType.PLAIN_WHITE)

                    val strokesList = mutableListOf<CanvasStroke>()
                    val strokesArray = pageObj.optJSONArray("strokes") ?: JSONArray()
                    for (j in 0 until strokesArray.length()) {
                        val sObj = strokesArray.getJSONObject(j)
                        val toolName = sObj.optString("toolType", DrawingToolType.PEN.name)
                        val toolType = runCatching { DrawingToolType.valueOf(toolName) }
                            .getOrDefault(DrawingToolType.PEN)
                        val ptsArray = sObj.optJSONArray("points") ?: JSONArray()
                        val pts = mutableListOf<PointFData>()
                        for (k in 0 until ptsArray.length()) {
                            val ptObj = ptsArray.getJSONObject(k)
                            pts.add(
                                PointFData(
                                    x = ptObj.optDouble("x", 0.0).toFloat(),
                                    y = ptObj.optDouble("y", 0.0).toFloat(),
                                    pressure = ptObj.optDouble("p", 1.0).toFloat()
                                )
                            )
                        }
                        strokesList.add(
                            CanvasStroke(
                                id = sObj.optString("id", UUID.randomUUID().toString()),
                                toolType = toolType,
                                colorArgb = sObj.optLong("colorArgb", 0xFF0F172AL),
                                strokeWidth = sObj.optDouble("strokeWidth", 6.0).toFloat(),
                                alpha = sObj.optDouble("alpha", 1.0).toFloat(),
                                points = pts,
                                polygonSides = sObj.optInt("polygonSides", 5)
                            )
                        )
                    }

                    val objectsList = mutableListOf<CanvasObject>()
                    val objectsArray = pageObj.optJSONArray("objects") ?: JSONArray()
                    for (j in 0 until objectsArray.length()) {
                        val oObj = objectsArray.getJSONObject(j)
                        val typeName = oObj.optString("type", CanvasObjectType.TEXT.name)
                        val objType = runCatching { CanvasObjectType.valueOf(typeName) }
                            .getOrDefault(CanvasObjectType.TEXT)
                        val mathName = oObj.optString("mathType", MathFormulaType.EQUATION.name)
                        val mathType = runCatching { MathFormulaType.valueOf(mathName) }
                            .getOrDefault(MathFormulaType.EQUATION)
                        objectsList.add(
                            CanvasObject(
                                id = oObj.optString("id", UUID.randomUUID().toString()),
                                type = objType,
                                x = oObj.optDouble("x", 300.0).toFloat(),
                                y = oObj.optDouble("y", 400.0).toFloat(),
                                scale = oObj.optDouble("scale", 1.0).toFloat(),
                                rotation = oObj.optDouble("rotation", 0.0).toFloat(),
                                colorArgb = oObj.optLong("colorArgb", 0xFF0F172AL),
                                text = oObj.optString("text", ""),
                                mathType = mathType,
                                primaryArg = oObj.optString("primaryArg", ""),
                                secondaryArg = oObj.optString("secondaryArg", ""),
                                tertiaryArg = oObj.optString("tertiaryArg", ""),
                                imagePath = oObj.optString("imagePath", ""),
                                isBakedToCanvas = oObj.optBoolean("isBakedToCanvas", false)
                            )
                        )
                    }

                    result.add(
                        BoardPage(
                            id = pageObj.optString("id", UUID.randomUUID().toString()),
                            backgroundType = bgType,
                            strokes = strokesList,
                            objects = objectsList
                        )
                    )
                }
                if (result.isEmpty()) listOf(BoardPage()) else result
            } catch (e: Exception) {
                listOf(BoardPage())
            }
        }
    }
}
