package com.douduoyu.app

import java.util.Collections

data class PaletteColor(
    val id: String,
    val code: String,
    val series: String,
    val argb: Int,
) {
    val red: Int = argb shr 16 and 0xFF
    val green: Int = argb shr 8 and 0xFF
    val blue: Int = argb and 0xFF
}

object BeadPalette {
    private data class RegisteredCustomColorScheme(
        val scheme: CustomColorScheme,
        val selectedColors: List<PaletteColor>,
    )

    private data class CustomColorRegistry(
        val schemesById: Map<String, RegisteredCustomColorScheme>,
        val colorsById: Map<String, PaletteColor>,
    ) {
        companion object {
            val Empty = CustomColorRegistry(emptyMap(), emptyMap())
        }
    }

    @Volatile
    private var customColorRegistry = CustomColorRegistry.Empty

    private val basicPaletteCsv =
        """
        A1,#FAF4C8
        A2,#FFFFD5
        A3,#FEFF8B
        A4,#FBED56
        A5,#F4D738
        A6,#FEAC4C
        A7,#FE8B4C
        A8,#FFDA45
        A9,#FF995B
        A10,#F77C31
        A11,#FFDD99
        A12,#FE9F72
        A13,#FFC365
        A14,#FD543D
        A15,#FFF365
        A16,#FFFF9F
        A17,#FFE36E
        A18,#FEBE7D
        A19,#FD7C72
        A20,#FFD568
        A21,#FFE395
        A22,#F4F57D
        A23,#E6C9B7
        A24,#F7F8A2
        A25,#FFD67D
        A26,#FFC830
        B1,#E6EE31
        B2,#63F347
        B3,#9EF780
        B4,#5DE035
        B5,#35E352
        B6,#65E2A6
        B7,#3DAF80
        B8,#1C9C4F
        B9,#27523A
        B10,#95D3C2
        B11,#5D722A
        B12,#166F41
        B13,#CAEB7B
        B14,#ADE946
        B15,#2E5132
        B16,#C5ED9C
        B17,#9BB13A
        B18,#E6EE49
        B19,#24B88C
        B20,#C2F0CC
        B21,#156A6B
        B22,#0B3C43
        B23,#303A21
        B24,#EEFCA5
        B25,#4E846D
        B26,#8D7A35
        B27,#CCE1AF
        B28,#9EE5B9
        B29,#C5E254
        B30,#E2FCB1
        B31,#B0E792
        B32,#9CAB5A
        C1,#E8FFE7
        C2,#A9F9FC
        C3,#A0E2FB
        C4,#41CCFF
        C5,#01ACEB
        C6,#50AAF0
        C7,#3677D2
        C8,#0F54C0
        C9,#324BCA
        C10,#3EBCE2
        C11,#28DDDE
        C12,#1C334D
        C13,#CDE8FF
        C14,#D5FDFF
        C15,#22C4C6
        C16,#1557A8
        C17,#04D1F6
        C18,#1D3344
        C19,#1887A2
        C20,#176DAF
        C21,#BEDDFF
        C22,#67B4BE
        C23,#C8E2FF
        C24,#7CC4FF
        C25,#A9E5E5
        C26,#3CAED8
        C27,#D3DFFA
        C28,#BBCFED
        C29,#34488E
        D1,#AEB4F2
        D2,#858EDD
        D3,#2F54AF
        D4,#182A84
        D5,#B843C5
        D6,#AC7BDE
        D7,#8854B3
        D8,#E2D3FF
        D9,#D5B9F8
        D10,#361851
        D11,#B9BAE1
        D12,#DE9AD4
        D13,#B90095
        D14,#8B279B
        D15,#2F1F90
        D16,#E3E1EE
        D17,#C4D4F6
        D18,#A45EC7
        D19,#D8C3D7
        D20,#9C32B2
        D21,#9A009B
        D22,#333A95
        D23,#EBDAFC
        D24,#7786E5
        D25,#494FC7
        D26,#DFC2F8
        E1,#FDD3CC
        E2,#FEC0DF
        E3,#FFB7E7
        E4,#E8649E
        E5,#F551A2
        E6,#F13D74
        E7,#C63478
        E8,#FFDBE9
        E9,#E970CC
        E10,#D33793
        E11,#FCDDD2
        E12,#F78FC3
        E13,#B5006D
        E14,#FFD1BA
        E15,#F8C7C9
        E16,#FFF3EB
        E17,#FFE2EA
        E18,#FFC7DB
        E19,#FEBAD5
        E20,#D8C7D1
        E21,#BD9DA1
        E22,#B785A1
        E23,#937A8D
        E24,#E1BCE8
        F1,#FD957B
        F2,#FC3D46
        F3,#F74941
        F4,#FC283C
        F5,#E7002F
        F6,#943630
        F7,#971937
        F8,#BC0028
        F9,#E2677A
        F10,#8A4526
        F11,#5A2121
        F12,#FD4E6A
        F13,#F35744
        F14,#FFA9AD
        F15,#D30022
        F16,#FEC2A6
        F17,#E69C79
        F18,#D37C46
        F19,#C1444A
        F20,#CD9391
        F21,#F7B4C6
        F22,#FDC0D0
        F23,#F67E66
        F24,#E698AA
        F25,#E54B4F
        G1,#FFE2CE
        G2,#FFC4AA
        G3,#F4C3A5
        G4,#E1B383
        G5,#EDB045
        G6,#E99C17
        G7,#9D5B3E
        G8,#753832
        G9,#E6B483
        G10,#D98C39
        G11,#E0C593
        G12,#FFC890
        G13,#B7714A
        G14,#8D614C
        G15,#FCF9E0
        G16,#F2D9BA
        G17,#78524B
        G18,#FFE4CC
        G19,#E07935
        G20,#A94023
        G21,#B88558
        H1,#FDFBFF
        H2,#FEFFFF
        H3,#B6B1BA
        H4,#89858C
        H5,#48464E
        H6,#2F2B2F
        H7,#000000
        H8,#E7D6DB
        H9,#EDEDED
        H10,#EEE9EA
        H11,#CECDD5
        H12,#FFF5ED
        H13,#F5ECD2
        H14,#CFD7D3
        H15,#98A6A8
        H16,#1D1414
        H17,#F1EDED
        H18,#FFFBE1
        H19,#F6EFE2
        H20,#949FA3
        H21,#FFFBE1
        H22,#CACAD4
        H23,#9A9D94
        M1,#BCC6B8
        M2,#8AA386
        M3,#697D80
        M4,#E3D2BC
        M5,#D0CCAA
        M6,#B0A782
        M7,#B4A497
        M8,#B38281
        M9,#A58767
        M10,#C5B2BC
        M11,#9F7594
        M12,#644749
        M13,#D19066
        M14,#C77362
        M15,#757D78
        """.trimIndent()

    private val extendedPaletteCsv =
        """
        P1,#FCF7F8
        P2,#B0A9AC
        P3,#AFDCAB
        P4,#FEA49F
        P5,#EE8C3E
        P6,#5FD0A7
        P7,#EB9270
        P8,#F0D958
        P9,#D9D9D9
        P10,#D9C7EA
        P11,#F3ECC9
        P12,#E6EEF2
        P13,#AACBEF
        P14,#337680
        P15,#668575
        P16,#FEBF45
        P17,#FEA324
        P18,#FEB89F
        P19,#FFFEEC
        P20,#FEBECF
        P21,#ECBEBF
        P22,#E4A89F
        P23,#A56268
        Q1,#F2A5E8
        Q2,#E9EC91
        Q3,#FFFF00
        Q4,#FFEBFA
        Q5,#76CEDE
        R1,#D50D21
        R2,#F92F83
        R3,#FD8324
        R4,#F8EC31
        R5,#35C75B
        R6,#238891
        R7,#19779D
        R8,#1A60C3
        R9,#9A56B4
        R10,#FFDB4C
        R11,#FFEBFA
        R12,#D8D5CE
        R13,#55514C
        R14,#9FE4DF
        R15,#77CEE9
        R16,#3ECFCA
        R17,#4A867A
        R18,#7FCD9D
        R19,#CDE55D
        R20,#E8C7B4
        R21,#AD6F3C
        R22,#6C372F
        R23,#FEB872
        R24,#F3C1C0
        R25,#C9675E
        R26,#D293BE
        R27,#EA8CB1
        R28,#9C87D6
        T1,#FFFFFF
        Y1,#FD6FB4
        Y2,#FEB481
        Y3,#D7FAA0
        Y4,#8BDBFA
        Y5,#E987EA
        ZG1,#DAABB3
        ZG2,#D6AA87
        ZG3,#C1BD8D
        ZG4,#96869F
        ZG5,#8490A6
        ZG6,#94BFE2
        ZG7,#E2A9D2
        ZG8,#AB91C0
        """.trimIndent()

    val basicColors: List<PaletteColor> = parsePalette(basicPaletteCsv)
    val completeColors: List<PaletteColor> = parsePalette(buildString {
        appendLine(basicPaletteCsv)
        append(extendedPaletteCsv)
    })
    val colors: List<PaletteColor> = basicColors

    private val colorMap by lazy {
        (completeColors + BrandPaletteData.schemePalettes.values.flatten())
            .associateBy { it.id }
    }
    private val legacySchemeCodes =
        mapOf(
            ColorSchemePreset.Mard24 to
                listOf(
                    "B3", "C3", "D9", "E2", "G1", "A4", "B5", "C5", "D6", "E4", "G5", "A6",
                    "B8", "C8", "D7", "F5", "G7", "A7", "H1", "H2", "H3", "H4", "H5", "H7",
                ),
            ColorSchemePreset.Mard48 to
                listOf(
                    "A4", "A6", "A7", "H7", "G1", "G5", "G7", "H5", "E2", "E4", "F5", "H4",
                    "D9", "D6", "D7", "H3", "C3", "C5", "C8", "H2", "B3", "B5", "B8", "H1",
                    "A11", "G9", "G13", "G8", "A13", "A10", "F13", "F8", "E8", "E3", "D13", "E7",
                    "D19", "D18", "D21", "D15", "C13", "C6", "C7", "D3", "C2", "C10", "C11", "B12",
                ),
            ColorSchemePreset.Mard72 to
                listOf(
                    "B3", "C3", "D9", "E2", "G1", "A4", "B5", "C5", "D6", "E4", "G5", "A6",
                    "B8", "C8", "D7", "F5", "G7", "A7", "H1", "H2", "H3", "H4", "H5", "H7",
                    "C2", "C13", "D19", "E8", "A13", "A11", "C10", "C6", "D18", "E3", "A10", "G9",
                    "C11", "C7", "D21", "D13", "F13", "G13", "B12", "D3", "D15", "E7", "F8", "G8",
                    "A3", "B20", "D16", "D8", "E1", "G2", "B18", "B10", "D11", "D12", "E12", "G3",
                    "B14", "B19", "D2", "D20", "E5", "F10", "B17", "B7", "C16", "D14", "E13", "F7",
                ),
            ColorSchemePreset.Mard96 to
                listOf(
                    "B3", "C3", "D9", "E2", "G1", "A4", "B5", "C5", "D6", "E4", "G5", "A6",
                    "B8", "C8", "D7", "F5", "G7", "A7", "H1", "H2", "H3", "H4", "H5", "H7",
                    "C2", "C13", "D19", "E8", "A13", "A11", "C10", "C6", "D18", "E3", "A10", "G9",
                    "C11", "C7", "D21", "D13", "F13", "G13", "B12", "D3", "D15", "E7", "F8", "G8",
                    "A3", "B20", "D16", "D8", "E1", "G2", "B18", "B10", "D11", "D12", "E12", "G3",
                    "B14", "B19", "D2", "D20", "E5", "F10", "B17", "B7", "C16", "D14", "E13", "F7",
                    "E11", "E14", "F1", "A14", "M6", "M5", "E15", "F14", "F9", "F2", "G14", "M9",
                    "E9", "E6", "F12", "F3", "F11", "M12", "D5", "E10", "F4", "F6", "G17", "H6",
                ),
            ColorSchemePreset.Mard120 to
                listOf(
                    "A4", "A6", "A7", "H7", "G1", "G5", "G7", "H5", "E2", "E4", "F5", "H4",
                    "D9", "D6", "D7", "H3", "C3", "C5", "C8", "H2", "B3", "B5", "B8", "H1",
                    "A11", "G9", "G13", "G8", "A13", "A10", "F13", "F8", "E8", "E3", "D13", "E7",
                    "D19", "D18", "D21", "D15", "C13", "C6", "C7", "D3", "C2", "C10", "C11", "B12",
                    "G2", "G3", "F10", "F7", "E1", "E12", "E5", "E13", "D8", "D12", "D20", "D14",
                    "D16", "D11", "D2", "C16", "B20", "B10", "B19", "B7", "A3", "B18", "B14", "B17",
                    "M5", "M9", "M12", "H6", "M6", "G14", "F11", "G17", "A14", "F2", "F3", "F6",
                    "F1", "F9", "F12", "F4", "E14", "F14", "E6", "E10", "E11", "E15", "E9", "D5",
                    "G6", "B11", "B15", "C9", "A9", "B4", "C15", "C17", "A12", "B2", "B6", "C4",
                    "A8", "B1", "B16", "D1", "A5", "B13", "C1", "D17", "A15", "A1", "H12", "C14",
                ),
            ColorSchemePreset.Mard144 to
                listOf(
                    "D17", "D11", "D2", "C16", "D16", "D1", "C7", "D3", "C13", "C17", "C6", "C9",
                    "C3", "C10", "C5", "C8", "C2", "C4", "C11", "B7", "B10", "B6", "C15", "B19",
                    "D9", "D6", "D18", "D15", "D8", "D12", "D20", "D7", "D19", "E9", "D13", "D14",
                    "E8", "E3", "D5", "D21", "E2", "E4", "E10", "E13", "E12", "E6", "E5", "E7",
                    "M6", "F10", "F11", "G8", "M5", "G13", "G7", "B11", "B18", "B1", "B14", "B17",
                    "C1", "B13", "B2", "B8", "B20", "B16", "B4", "B12", "C14", "B3", "B5", "B15",
                    "F12", "F3", "F6", "F7", "F14", "F2", "F9", "F8", "A9", "F1", "F13", "F5",
                    "A11", "A6", "A7", "F4", "A3", "A13", "A10", "A14", "A15", "A4", "A5", "A8",
                    "H1", "H12", "H4", "H7", "H2", "A1", "H3", "H6", "E11", "G1", "M9", "H5",
                    "E14", "G2", "G9", "G17", "E1", "G3", "G5", "M12", "E15", "A12", "G6", "G14",
                    "H9", "H14", "H11", "M15", "G16", "M4", "M7", "M8", "H13", "G4", "M13", "M14",
                    "A2", "G11", "G12", "G10", "G15", "M1", "M2", "M3", "H8", "H10", "M10", "M11",
                ),
            ColorSchemePreset.Mard168 to
                listOf(
                    "D17", "D11", "D2", "C16", "D16", "D1", "C7", "D3", "C13", "C17", "C6", "C9",
                    "C3", "C10", "C5", "C8", "C2", "C4", "C11", "B7", "B10", "B6", "C15", "B19",
                    "D9", "D6", "D18", "D15", "D8", "D12", "D20", "D7", "D19", "E9", "D13", "D14",
                    "E8", "E3", "D5", "D21", "E2", "E4", "E10", "E13", "E12", "E6", "E5", "E7",
                    "M6", "F10", "F11", "G8", "M5", "G13", "G7", "B11", "B18", "B1", "B14", "B17",
                    "C1", "B13", "B2", "B8", "B20", "B16", "B4", "B12", "C14", "B3", "B5", "B15",
                    "F12", "F3", "F6", "F7", "F14", "F2", "F9", "F8", "A9", "F1", "F13", "F5",
                    "A11", "A6", "A7", "F4", "A3", "A13", "A10", "A14", "A15", "A4", "A5", "A8",
                    "H1", "H12", "H4", "H7", "H2", "A1", "H3", "H6", "E11", "G1", "M9", "H5",
                    "E14", "G2", "G9", "G17", "E1", "G3", "G5", "M12", "E15", "A12", "G6", "G14",
                    "H9", "H14", "H11", "M15", "G16", "M4", "M7", "M8", "H13", "G4", "M13", "M14",
                    "A2", "G11", "G12", "G10", "G15", "M1", "M2", "M3", "H8", "H10", "M10", "M11",
                    "A16", "A17", "A18", "A19", "A20", "A21", "A22", "A23", "A24", "A25", "A26", "B9",
                    "B21", "B22", "B23", "B24", "B25", "B26", "B27", "B28", "B29", "B30", "B31", "B32",
                ),
            ColorSchemePreset.Mard221 to
                listOf(
                    "D17", "D11", "D2", "C16", "D16", "D1", "C7", "D3", "C13", "C17", "C6", "C9",
                    "C3", "C10", "C5", "C8", "C2", "C4", "C11", "B7", "B10", "B6", "C15", "B19",
                    "D9", "D6", "D18", "D15", "D8", "D12", "D20", "D7", "D19", "E9", "D13", "D14",
                    "E8", "E3", "D5", "D21", "E2", "E4", "E10", "E13", "E12", "E6", "E5", "E7",
                    "M6", "F10", "F11", "G8", "M5", "G13", "G7", "B11", "B18", "B1", "B14", "B17",
                    "C1", "B13", "B2", "B8", "B20", "B16", "B4", "B12", "C14", "B3", "B5", "B15",
                    "F12", "F3", "F6", "F7", "F14", "F2", "F9", "F8", "A9", "F1", "F13", "F5",
                    "A11", "A6", "A7", "F4", "A3", "A13", "A10", "A14", "A15", "A4", "A5", "A8",
                    "H1", "H12", "H4", "H7", "H2", "A1", "H3", "H6", "E11", "G1", "M9", "H5",
                    "E14", "G2", "G9", "G17", "E1", "G3", "G5", "M12", "E15", "A12", "G6", "G14",
                    "H9", "H14", "H11", "M15", "G16", "M4", "M7", "M8", "H13", "G4", "M13", "M14",
                    "A2", "G11", "G12", "G10", "G15", "M1", "M2", "M3", "H8", "H10", "M10", "M11",
                    "A16", "A17", "A18", "A19", "A20", "A21", "A22", "A23", "A24", "A25", "A26", "B9",
                    "B21", "B22", "B23", "B24", "B25", "B26", "B27", "B28", "B29", "B30", "B31", "B32",
                    "C12", "C18", "C19", "C20", "C21", "C22", "C23", "C24", "C25", "C26", "C27", "C28",
                    "C29", "D4", "D10", "D22", "D23", "D24", "D25", "D26", "E16", "E17", "E18", "E19",
                    "E20", "E21", "E22", "E23", "E24", "F15", "F16", "F17", "F18", "F19", "F20", "F21",
                    "F22", "F23", "F24", "F25", "G18", "G19", "G20", "G21", "H15", "H16", "H17", "H18",
                    "H19", "H20", "H21", "H22", "H23",
                ),
        )

    fun palette(mode: PaletteMode): List<PaletteColor> {
        return if (mode == PaletteMode.Complete) completeColors else basicColors
    }

    fun palette(scheme: ColorSchemePreset): List<PaletteColor> {
        BrandPaletteData.schemePalettes[scheme]?.let { return it }
        if (scheme == ColorSchemePreset.Complete291) {
            return completeColors
        }
        return legacySchemeCodes[scheme]
            ?.mapNotNull(colorMap::get)
            ?.takeIf { it.isNotEmpty() }
            ?: palette(scheme.paletteMode)
    }

    fun palette(
        baseScheme: ColorSchemePreset,
        customId: String?,
    ): List<PaletteColor> {
        val registered = customId?.let(customColorRegistry.schemesById::get)
        return registered
            ?.selectedColors
            ?.takeIf { it.isNotEmpty() }
            ?: palette(baseScheme)
    }

    fun registerCustomColorSchemes(schemes: List<CustomColorScheme>) {
        val normalizedById = linkedMapOf<String, CustomColorScheme>()
        schemes.forEach { rawScheme ->
            rawScheme.normalizedOrNull()?.let { scheme ->
                normalizedById[scheme.id] = immutableSchemeSnapshot(scheme)
            }
        }

        val registeredById = linkedMapOf<String, RegisteredCustomColorScheme>()
        val customColorsById = linkedMapOf<String, PaletteColor>()
        normalizedById.values.forEach { scheme ->
            val allColors = immutableListSnapshot(scheme.palette(includeDisabled = true))
            allColors.forEach { color -> customColorsById[color.id] = color }
            val selectedCodes = scheme.selectedCodes.toHashSet()
            registeredById[scheme.id] =
                RegisteredCustomColorScheme(
                    scheme = scheme,
                    selectedColors = immutableListSnapshot(allColors.filter { it.code in selectedCodes }),
                )
        }

        customColorRegistry =
            CustomColorRegistry(
                schemesById = Collections.unmodifiableMap(registeredById),
                colorsById = Collections.unmodifiableMap(customColorsById),
            )
    }

    fun customColorScheme(id: String?): CustomColorScheme? {
        return id?.let(customColorRegistry.schemesById::get)?.scheme
    }

    fun schemeLabel(
        base: ColorSchemePreset,
        customId: String?,
    ): String = customColorScheme(customId)?.name ?: base.label

    fun normalizeStoredCode(
        code: String?,
        scheme: ColorSchemePreset,
    ): String? {
        if (code == null) {
            return null
        }
        BrandPaletteData.legacyBrandCodeAliasesByScheme[scheme]?.get(code)?.let { return it }
        BrandPaletteData.displayCodeAliasesByScheme[scheme]?.get(code)?.let { return it }
        return code.takeIf { find(it) != null }
            ?: code
    }

    fun find(code: String?): PaletteColor? {
        if (code == null) {
            return null
        }
        return customColorRegistry.colorsById[code] ?: colorMap[code]
    }

    fun displayCode(code: String?): String = find(code)?.code ?: code.orEmpty()

    fun nearest(
        red: Int,
        green: Int,
        blue: Int,
        candidates: List<PaletteColor> = colors,
    ): PaletteColor {
        val palette = candidates.ifEmpty { colors }
        return palette[nearestIndex(red, green, blue, palette)]
    }

    internal fun nearestIndex(
        red: Int,
        green: Int,
        blue: Int,
        candidates: List<PaletteColor> = colors,
    ): Int {
        val palette = candidates.ifEmpty { colors }
        var bestIndex = 0
        var bestDistance = Double.POSITIVE_INFINITY

        for (index in palette.indices) {
            val color = palette[index]
            val distance = colorDistanceSquared(red, green, blue, color)
            if (distance < bestDistance) {
                bestDistance = distance
                bestIndex = index
            }
        }

        return bestIndex
    }

    fun distanceBetween(left: String, right: String): Double {
        val leftColor = find(left) ?: return Double.POSITIVE_INFINITY
        val rightColor = find(right) ?: return Double.POSITIVE_INFINITY
        return colorDistance(
            intArrayOf(leftColor.red, leftColor.green, leftColor.blue),
            intArrayOf(rightColor.red, rightColor.green, rightColor.blue),
        )
    }

    private fun parsePalette(csv: String): List<PaletteColor> {
        return csv.lineSequence()
            .map(String::trim)
            .filter { it.isNotEmpty() }
            .map { line ->
                val (code, hex) = line.split(",", limit = 2)
                val cleanCode = code.trim()
                PaletteColor(
                    id = cleanCode,
                    code = cleanCode,
                    series = if (cleanCode.startsWith("ZG")) "ZG" else cleanCode.first().toString(),
                    argb = hexToArgb(hex.trim()),
                )
            }
            .toList()
    }

    private fun immutableSchemeSnapshot(scheme: CustomColorScheme): CustomColorScheme {
        return scheme.copy(
            selectedCodes = immutableListSnapshot(scheme.selectedCodes),
            colorOverrides = Collections.unmodifiableMap(LinkedHashMap(scheme.colorOverrides)),
        )
    }

    private fun <T> immutableListSnapshot(values: Collection<T>): List<T> {
        return Collections.unmodifiableList(ArrayList(values))
    }

    internal fun hexToArgb(hex: String): Int {
        val clean = hex.removePrefix("#")
        val value = clean.toLong(16)
        return when (clean.length) {
            6 -> (0xFF000000 or value).toInt()
            8 -> (((value and 0xFF) shl 24) or (value ushr 8)).toInt()
            else -> throw IllegalArgumentException("Expected #RRGGBB or #RRGGBBAA: $hex")
        }
    }

    private fun colorDistance(left: IntArray, right: IntArray): Double {
        val redMean = (left[0] + right[0]) / 2.0
        val deltaRed = (left[0] - right[0]).toDouble()
        val deltaGreen = (left[1] - right[1]).toDouble()
        val deltaBlue = (left[2] - right[2]).toDouble()
        return kotlin.math.sqrt(
            (2 + redMean / 256.0) * deltaRed * deltaRed +
                4 * deltaGreen * deltaGreen +
                (2 + (255 - redMean) / 256.0) * deltaBlue * deltaBlue,
        )
    }

    private fun colorDistanceSquared(
        red: Int,
        green: Int,
        blue: Int,
        color: PaletteColor,
    ): Double {
        val redMean = (red + color.red) / 2.0
        val deltaRed = (red - color.red).toDouble()
        val deltaGreen = (green - color.green).toDouble()
        val deltaBlue = (blue - color.blue).toDouble()
        return (2 + redMean / 256.0) * deltaRed * deltaRed +
            4 * deltaGreen * deltaGreen +
            (2 + (255 - redMean) / 256.0) * deltaBlue * deltaBlue
    }
}
