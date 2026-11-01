package com.example.model

import java.util.UUID

enum class DrawingToolType {
    PEN,
    PENCIL,
    BRUSH,
    HIGHLIGHTER,
    MARKER,
    NEON_PEN,
    PIXEL_ERASER,
    STROKE_ERASER,
    LINE,
    ARROW,
    CIRCLE,
    RECTANGLE,
    TRIANGLE,
    POLYGON,
    SELECT_MOVE,
    HAND_PAN
}

enum class BackgroundType {
    PLAIN_WHITE,
    SQUARE_GRID,
    DOT_GRID,
    RULED_LINES,
    COORDINATE_GRAPH
}

enum class MathFormulaType {
    FRACTION,
    SQUARE_ROOT,
    CUBE_ROOT,
    POWER,
    SUBSCRIPT,
    INTEGRAL,
    SUMMATION,
    EQUATION
}

enum class CanvasObjectType {
    TEXT,
    MATH_FORMULA,
    IMAGE
}

data class PointFData(
    val x: Float,
    val y: Float,
    val pressure: Float = 1f
)

data class CanvasStroke(
    val id: String = UUID.randomUUID().toString(),
    val toolType: DrawingToolType,
    val colorArgb: Long,
    val strokeWidth: Float,
    val alpha: Float = 1f,
    val points: List<PointFData>,
    val polygonSides: Int = 5
)

data class CanvasObject(
    val id: String = UUID.randomUUID().toString(),
    val type: CanvasObjectType,
    val x: Float,
    val y: Float,
    val scale: Float = 1f,
    val rotation: Float = 0f,
    val colorArgb: Long = 0xFF0F172AL,
    val text: String = "",
    val mathType: MathFormulaType = MathFormulaType.EQUATION,
    val primaryArg: String = "",
    val secondaryArg: String = "",
    val tertiaryArg: String = "",
    val imagePath: String = "",
    val isBakedToCanvas: Boolean = false
)

data class BoardPage(
    val id: String = UUID.randomUUID().toString(),
    val backgroundType: BackgroundType = BackgroundType.PLAIN_WHITE,
    val strokes: List<CanvasStroke> = emptyList(),
    val objects: List<CanvasObject> = emptyList()
)

data class RulerState(
    val enabled: Boolean = false,
    val centerX: Float = 540f,
    val centerY: Float = 760f,
    val angleDegrees: Float = 0f,
    val lengthPx: Float = 720f
)

data class ProtractorState(
    val enabled: Boolean = false,
    val centerX: Float = 540f,
    val centerY: Float = 820f,
    val rotationDegrees: Float = 0f,
    val radiusPx: Float = 310f,
    val guideAngleDegrees: Float = 45f
)

data class CompassState(
    val enabled: Boolean = false,
    val pivotX: Float = 500f,
    val pivotY: Float = 780f,
    val radiusPx: Float = 220f,
    val startAngleDegrees: Float = 0f,
    val sweepAngleDegrees: Float = 360f
)

data class AlgebraicFormulaPreset(
    val categoryEn: String,
    val categoryBn: String,
    val titleEn: String,
    val titleBn: String,
    val mathType: MathFormulaType,
    val primaryArg: String,
    val secondaryArg: String = "",
    val tertiaryArg: String = ""
)

object MathCatalog {
    val formulaPresets: List<AlgebraicFormulaPreset> = listOf(
        AlgebraicFormulaPreset(
            categoryEn = "Algebra",
            categoryBn = "বীজগণিত",
            titleEn = "Square of Sum",
            titleBn = "বর্গের সূত্র ১",
            mathType = MathFormulaType.EQUATION,
            primaryArg = "(a + b)² = a² + 2ab + b²"
        ),
        AlgebraicFormulaPreset(
            categoryEn = "Algebra",
            categoryBn = "বীজগণিত",
            titleEn = "Square of Difference",
            titleBn = "বর্গের সূত্র ২",
            mathType = MathFormulaType.EQUATION,
            primaryArg = "(a − b)² = a² − 2ab + b²"
        ),
        AlgebraicFormulaPreset(
            categoryEn = "Algebra",
            categoryBn = "বীজগণিত",
            titleEn = "Difference of Squares",
            titleBn = "বর্গের অন্তর",
            mathType = MathFormulaType.EQUATION,
            primaryArg = "a² − b² = (a + b)(a − b)"
        ),
        AlgebraicFormulaPreset(
            categoryEn = "Algebra",
            categoryBn = "বীজগণিত",
            titleEn = "Cube of Sum",
            titleBn = "ঘনের সূত্র",
            mathType = MathFormulaType.EQUATION,
            primaryArg = "(a + b)³ = a³ + 3a²b + 3ab² + b³"
        ),
        AlgebraicFormulaPreset(
            categoryEn = "Algebra",
            categoryBn = "বীজগণিত",
            titleEn = "Quadratic Formula",
            titleBn = "দ্বিঘাত সমীকরণ",
            mathType = MathFormulaType.FRACTION,
            primaryArg = "−b ± √(b² − 4ac)",
            secondaryArg = "2a"
        ),
        AlgebraicFormulaPreset(
            categoryEn = "Geometry",
            categoryBn = "জ্যামিতি",
            titleEn = "Pythagorean Theorem",
            titleBn = "পিথাগোরাসের উপপাদ্য",
            mathType = MathFormulaType.EQUATION,
            primaryArg = "a² + b² = c²"
        ),
        AlgebraicFormulaPreset(
            categoryEn = "Geometry",
            categoryBn = "জ্যামিতি",
            titleEn = "Distance Formula",
            titleBn = "দূরত্বের সূত্র",
            mathType = MathFormulaType.SQUARE_ROOT,
            primaryArg = "(x₂ − x₁)² + (y₂ − y₁)²"
        ),
        AlgebraicFormulaPreset(
            categoryEn = "Trigonometry",
            categoryBn = "ত্রিকোণমিতি",
            titleEn = "Pythagorean Identity",
            titleBn = "ত্রিকোণমিতিক অভেদ",
            mathType = MathFormulaType.EQUATION,
            primaryArg = "sin²θ + cos²θ = 1"
        ),
        AlgebraicFormulaPreset(
            categoryEn = "Trigonometry",
            categoryBn = "ত্রিকোণমিতি",
            titleEn = "Sine Double Angle",
            titleBn = "গুণিতক কোণ",
            mathType = MathFormulaType.EQUATION,
            primaryArg = "sin(2θ) = 2 sinθ cosθ"
        ),
        AlgebraicFormulaPreset(
            categoryEn = "Calculus",
            categoryBn = "ক্যালকুলাস",
            titleEn = "Fundamental Integral",
            titleBn = "মৌলিক যোগজীকরণ",
            mathType = MathFormulaType.INTEGRAL,
            primaryArg = "xⁿ dx = xⁿ⁺¹ / (n+1)",
            secondaryArg = "a",
            tertiaryArg = "b"
        ),
        AlgebraicFormulaPreset(
            categoryEn = "Calculus",
            categoryBn = "ক্যালকুলাস",
            titleEn = "Gauss Summation",
            titleBn = "স্বাভাবিক সংখ্যার সমষ্টি",
            mathType = MathFormulaType.SUMMATION,
            primaryArg = "k = n(n + 1) / 2",
            secondaryArg = "k=1",
            tertiaryArg = "n"
        ),
        AlgebraicFormulaPreset(
            categoryEn = "Physics",
            categoryBn = "পদার্থবিজ্ঞান",
            titleEn = "Mass-Energy Equivalence",
            titleBn = "ভর-শক্তি সমীকরণ",
            mathType = MathFormulaType.POWER,
            primaryArg = "E = mc",
            secondaryArg = "2"
        )
    )

    val greekSymbols: List<Pair<String, String>> = listOf(
        "α" to "alpha", "β" to "beta", "γ" to "gamma", "δ" to "delta",
        "ε" to "epsilon", "ζ" to "zeta", "η" to "eta", "θ" to "theta",
        "λ" to "lambda", "μ" to "mu", "ν" to "nu", "ξ" to "xi",
        "π" to "pi", "ρ" to "rho", "σ" to "sigma", "τ" to "tau",
        "φ" to "phi", "χ" to "chi", "ψ" to "psi", "ω" to "omega",
        "Δ" to "Delta", "Σ" to "Sigma", "Π" to "Pi", "Ω" to "Omega"
    )

    val mathOperators: List<Pair<String, String>> = listOf(
        "±" to "Plus-Minus", "×" to "Multiply", "÷" to "Divide", "≠" to "Not Equal",
        "≈" to "Approx", "≡" to "Identical", "≤" to "Less/Equal", "≥" to "Greater/Equal",
        "∞" to "Infinity", "√" to "Square Root", "∛" to "Cube Root", "∫" to "Integral",
        "∑" to "Summation", "∂" to "Partial", "∇" to "Nabla", "∝" to "Proportional",
        "∈" to "Element Of", "∉" to "Not In", "⊂" to "Subset", "∪" to "Union",
        "∩" to "Intersection", "∅" to "Empty Set", "∴" to "Therefore", "∵" to "Because",
        "∠" to "Angle", "⊥" to "Perpendicular", "∥" to "Parallel", "△" to "Triangle"
    )
}
