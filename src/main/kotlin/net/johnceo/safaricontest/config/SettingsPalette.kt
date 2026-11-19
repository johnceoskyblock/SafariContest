package net.johnceo.safaricontest.config

/** SafariUtils settings role palettes, adapted under MIT (licenses/SafariUtils-MIT.txt). */
object SettingsPalette {
    val names = listOf("Default", "Midnight", "Canyon", "Forest", "Rainbow", "Amethyst", "Ocean", "Rose", "Arctic", "Cherry Blossom", "Ember", "Ender", "Golden Hour", "Monochrome", "Nebula", "Slate", "Blueprint", "Coffee", "Cyberpunk", "Desert", "Jade", "Paper", "Solarized", "Terminal", "Aurora", "Candy", "Copper", "Deep Sea", "Frostfire", "Lavender", "Matrix", "Royal", "Sunset", "Vaporwave")
    private val palettes = arrayOf(
        intArrayOf(0xF00B0D13.toInt(),0xE611141C.toInt(),0xD9181B24.toInt(),0xE3222733.toInt(),0xFF30384A.toInt(),0xFF55AAFF.toInt(),0xFF55FFFF.toInt(),0xFF55FF88.toInt(),0xFFFF6677.toInt(),0xFFFFC857.toInt(),0xFFF2F5FA.toInt(),0xFF9DA7B8.toInt(),0xFF697386.toInt()),
        intArrayOf(0xF0060810.toInt(),0xEB0C1020.toInt(),0xDC11172A.toInt(),0xEB18213A.toInt(),0xFF2A3557.toInt(),0xFF7A8CFF.toInt(),0xFFA6B3FF.toInt(),0xFF67E8A5.toInt(),0xFFFF6F91.toInt(),0xFFB8A7FF.toInt(),0xFFF5F6FF.toInt(),0xFFA2A9C2.toInt(),0xFF69708B.toInt()),
        intArrayOf(0xF0120B08.toInt(),0xEB21120D.toInt(),0xDC2B1810.toInt(),0xEB392116.toInt(),0xFF66402A.toInt(),0xFFFF8C42.toInt(),0xFFFFB35C.toInt(),0xFF8FDB76.toInt(),0xFFFF635D.toInt(),0xFFFFD166.toInt(),0xFFFFF4E8.toInt(),0xFFC6A993.toInt(),0xFF846A59.toInt()),
        intArrayOf(0xF0050D0A.toInt(),0xEB0A1912.toInt(),0xDC10241A.toInt(),0xEB173326.toInt(),0xFF28543E.toInt(),0xFF55B98A.toInt(),0xFF72E6AC.toInt(),0xFF7DFF9B.toInt(),0xFFFF7272.toInt(),0xFFE8C96A.toInt(),0xFFF0FFF7.toInt(),0xFF9CC2AD.toInt(),0xFF648272.toInt()),
        intArrayOf(0xF00A0911.toInt(),0xEB151220.toInt(),0xDC1B1728.toInt(),0xEB29213B.toInt(),0xFF4C4165.toInt(),0xFFB68CFF.toInt(),0xFFB68CFF.toInt(),0xFF70FF9A.toInt(),0xFFFF708B.toInt(),0xFFB68CFF.toInt(),0xFFFFFFFF.toInt(),0xFFBBB4CC.toInt(),0xFF776F89.toInt()),
        intArrayOf(0xF00D0813.toInt(),0xEB1A1026.toInt(),0xDC251735.toInt(),0xEB322047.toInt(),0xFF56376F.toInt(),0xFFB66DFF.toInt(),0xFFD69BFF.toInt(),0xFF7DFFB3.toInt(),0xFFFF719E.toInt(),0xFFE5B8FF.toInt(),0xFFFFF5FF.toInt(),0xFFC4A9D2.toInt(),0xFF806B8D.toInt()),
        intArrayOf(0xF0040E15.toInt(),0xEB071B27.toInt(),0xDC0B2635.toInt(),0xEB103548.toInt(),0xFF20566D.toInt(),0xFF39A9DB.toInt(),0xFF61D8FF.toInt(),0xFF67F2C0.toInt(),0xFFFF738C.toInt(),0xFF8DE8FF.toInt(),0xFFF0FBFF.toInt(),0xFF9CBECB.toInt(),0xFF607E8B.toInt()),
        intArrayOf(0xF016070B.toInt(),0xEB2B0C12.toInt(),0xDC40101A.toInt(),0xEB571623.toInt(),0xFF8A263B.toInt(),0xFFFF3E68.toInt(),0xFFFF6F8F.toInt(),0xFF6FE39A.toInt(),0xFFFF4359.toInt(),0xFFFFA14A.toInt(),0xFFFFF1F3.toInt(),0xFFD4A0A8.toInt(),0xFF8B5A64.toInt()),
        intArrayOf(0xF0071017.toInt(),0xEB0D202C.toInt(),0xDC13303F.toInt(),0xEB1A4154.toInt(),0xFF39748B.toInt(),0xFF7DDBFF.toInt(),0xFFB0ECFF.toInt(),0xFF8DFFD5.toInt(),0xFFFF8298.toInt(),0xFFD6F5FF.toInt(),0xFFF4FCFF.toInt(),0xFFA9CAD6.toInt(),0xFF6B8994.toInt()),
        intArrayOf(0xF0140B13.toInt(),0xEB271424.toInt(),0xDC361B32.toInt(),0xEB482443.toInt(),0xFF723C68.toInt(),0xFFFF82B2.toInt(),0xFFFFC0D9.toInt(),0xFF88E8B1.toInt(),0xFFFF718A.toInt(),0xFFDDB7FF.toInt(),0xFFFFF6FA.toInt(),0xFFD1AEBE.toInt(),0xFF896D7A.toInt()),
        intArrayOf(0xF0150804.toInt(),0xEB2B1008.toInt(),0xDC3D170B.toInt(),0xEB522012.toInt(),0xFF813A1D.toInt(),0xFFFF6B2C.toInt(),0xFFFF9D52.toInt(),0xFF9DE06F.toInt(),0xFFFF554F.toInt(),0xFFFFC15A.toInt(),0xFFFFF3E8.toInt(),0xFFD0A088.toInt(),0xFF8C6551.toInt()),
        intArrayOf(0xF00B0613.toInt(),0xEB170C29.toInt(),0xDC23123B.toInt(),0xEB301950.toInt(),0xFF593181.toInt(),0xFFA85CFF.toInt(),0xFFD190FF.toInt(),0xFF67ECA2.toInt(),0xFFFF638D.toInt(),0xFFE3AEFF.toInt(),0xFFFCF4FF.toInt(),0xFFBFA7CF.toInt(),0xFF79668A.toInt()),
        intArrayOf(0xF0120E05.toInt(),0xEB271D08.toInt(),0xDC382A0C.toInt(),0xEB4A3811.toInt(),0xFF795E26.toInt(),0xFFFFC642.toInt(),0xFFFFDD76.toInt(),0xFF9BE58C.toInt(),0xFFFF6D64.toInt(),0xFFFFE09A.toInt(),0xFFFFF9E8.toInt(),0xFFD1C29A.toInt(),0xFF887C5C.toInt()),
        intArrayOf(0xF00B0B0B.toInt(),0xEB171717.toInt(),0xDC242424.toInt(),0xEB323232.toInt(),0xFF555555.toInt(),0xFFBDBDBD.toInt(),0xFFE0E0E0.toInt(),0xFF9AD5AC.toInt(),0xFFFF7B7B.toInt(),0xFFF2F2F2.toInt(),0xFFFFFFFF.toInt(),0xFFB8B8B8.toInt(),0xFF777777.toInt()),
        intArrayOf(0xF0080714.toInt(),0xEB121027.toInt(),0xDC1B1839.toInt(),0xEB27214E.toInt(),0xFF4A3D78.toInt(),0xFF776BFF.toInt(),0xFFC06CFF.toInt(),0xFF64E7B0.toInt(),0xFFFF668F.toInt(),0xFFFF9BDC.toInt(),0xFFF8F4FF.toInt(),0xFFB2A8CA.toInt(),0xFF716886.toInt()),
        intArrayOf(0xF00A0D11.toInt(),0xEB141A21.toInt(),0xDC202934.toInt(),0xEB2C3845.toInt(),0xFF4B5D70.toInt(),0xFF7A9AB8.toInt(),0xFFA9C3D9.toInt(),0xFF85D6A7.toInt(),0xFFFF7C83.toInt(),0xFFC7D8E5.toInt(),0xFFF4F7FA.toInt(),0xFFAAB5BF.toInt(),0xFF6F7B86.toInt()),
        intArrayOf(0xF0041020.toInt(),0xEB071A31.toInt(),0xDC0B2645.toInt(),0xEB10345B.toInt(),0xFF23659B.toInt(),0xFF41A7FF.toInt(),0xFF83CEFF.toInt(),0xFF5DE3B2.toInt(),0xFFFF6E7E.toInt(),0xFFFFD166.toInt(),0xFFF3FAFF.toInt(),0xFF9CBAD0.toInt(),0xFF58768D.toInt()),
        intArrayOf(0xF0140D08.toInt(),0xEB25170F.toInt(),0xDC362117.toInt(),0xEB493022.toInt(),0xFF75513A.toInt(),0xFFC98A55.toInt(),0xFFE9B778.toInt(),0xFF9BCB75.toInt(),0xFFE96A58.toInt(),0xFFF1CF8A.toInt(),0xFFFFF4E5.toInt(),0xFFC9AE94.toInt(),0xFF88705D.toInt()),
        intArrayOf(0xF00F0615.toInt(),0xEB1E0A2B.toInt(),0xDC30103F.toInt(),0xEB461657.toInt(),0xFF7B258A.toInt(),0xFFFF2DCB.toInt(),0xFF20F6FF.toInt(),0xFF5DFF78.toInt(),0xFFFF4B3E.toInt(),0xFFFFFF38.toInt(),0xFFFFFFFF.toInt(),0xFFC9A5D2.toInt(),0xFF835B8F.toInt()),
        intArrayOf(0xF0171005.toInt(),0xEB2A200B.toInt(),0xDC3C3012.toInt(),0xEB51421C.toInt(),0xFF806B35.toInt(),0xFFE5A944.toInt(),0xFFF1D07A.toInt(),0xFF8BCB72.toInt(),0xFFD95B4C.toInt(),0xFFF6C85F.toInt(),0xFFFFF5D8.toInt(),0xFFCAB98A.toInt(),0xFF86774F.toInt()),
        intArrayOf(0xF004100D.toInt(),0xEB071F19.toInt(),0xDC0B3026.toInt(),0xEB104335.toInt(),0xFF24735B.toInt(),0xFF24C98A.toInt(),0xFF72E8B9.toInt(),0xFF8FFF76.toInt(),0xFFFF6B63.toInt(),0xFFFFD05A.toInt(),0xFFF1FFF8.toInt(),0xFF9CC8B3.toInt(),0xFF5D8873.toInt()),
        intArrayOf(0xF0EEE8D8.toInt(),0xEBE2DAC8.toInt(),0xD9D4CBB7.toInt(),0xEBC6BBA4.toInt(),0xFF9B8F76.toInt(),0xFF315A91.toInt(),0xFF287E8A.toInt(),0xFF39754A.toInt(),0xFFB43D3D.toInt(),0xFF996515.toInt(),0xFF25231F.toInt(),0xFF625E55.toInt(),0xFF8B8477.toInt()),
        intArrayOf(0xF0002B36.toInt(),0xEB073642.toInt(),0xDC0D414D.toInt(),0xEB164D58.toInt(),0xFF526C72.toInt(),0xFF268BD2.toInt(),0xFF2AA198.toInt(),0xFF859900.toInt(),0xFFDC322F.toInt(),0xFFB58900.toInt(),0xFFFDF6E3.toInt(),0xFF93A1A1.toInt(),0xFF657B83.toInt()),
        intArrayOf(0xF0010803.toInt(),0xEB031207.toInt(),0xDC061D0B.toInt(),0xEB092A10.toInt(),0xFF155C27.toInt(),0xFF20C95A.toInt(),0xFF62F58C.toInt(),0xFF8CFF63.toInt(),0xFFFF5D57.toInt(),0xFFE8F35A.toInt(),0xFFE8FFE9.toInt(),0xFF8FC49A.toInt(),0xFF50795A.toInt()),
        intArrayOf(0xF0040B18.toInt(),0xEB09172A.toInt(),0xDC10243C.toInt(),0xEB183552.toInt(),0xFF315F7E.toInt(),0xFF47C7B1.toInt(),0xFF85F0D1.toInt(),0xFF9BFF88.toInt(),0xFFFF719C.toInt(),0xFFB58CFF.toInt(),0xFFF1FFFC.toInt(),0xFF9EC7C2.toInt(),0xFF5F827F.toInt()),
        intArrayOf(0xF0180B18.toInt(),0xEB2A112A.toInt(),0xDC3A183B.toInt(),0xEB502451.toInt(),0xFF834A82.toInt(),0xFFFF71C8.toInt(),0xFF70E8FF.toInt(),0xFF8CFFBA.toInt(),0xFFFF6F91.toInt(),0xFFFFD36E.toInt(),0xFFFFF4FC.toInt(),0xFFD7A9CA.toInt(),0xFF916B8A.toInt()),
        intArrayOf(0xF0140B06.toInt(),0xEB26150D.toInt(),0xDC382016.toInt(),0xEB4C2D20.toInt(),0xFF79503C.toInt(),0xFFC97845.toInt(),0xFF4FC1B2.toInt(),0xFF8BD47C.toInt(),0xFFE85D4C.toInt(),0xFFE6A85C.toInt(),0xFFFFF1E4.toInt(),0xFFC6A58D.toInt(),0xFF846A58.toInt()),
        intArrayOf(0xF0010710.toInt(),0xEB031322.toInt(),0xDC071F34.toInt(),0xEB0B2D49.toInt(),0xFF174E70.toInt(),0xFF168AAD.toInt(),0xFF39E4E8.toInt(),0xFF6FE8A5.toInt(),0xFFFF627D.toInt(),0xFF69C8FF.toInt(),0xFFE9FFFF.toInt(),0xFF87B8C4.toInt(),0xFF4C7888.toInt()),
        intArrayOf(0xF00D0B15.toInt(),0xEB171526.toInt(),0xDC22213A.toInt(),0xEB302F50.toInt(),0xFF535485.toInt(),0xFF63B8FF.toInt(),0xFFFF7A45.toInt(),0xFF7FE4A1.toInt(),0xFFFF5665.toInt(),0xFFFFB15C.toInt(),0xFFF5F7FF.toInt(),0xFFAAAEC7.toInt(),0xFF6B708C.toInt()),
        intArrayOf(0xF0120D1A.toInt(),0xEB21182D.toInt(),0xDC302440.toInt(),0xEB433459.toInt(),0xFF705D8A.toInt(),0xFFA98ADB.toInt(),0xFFD3B8FF.toInt(),0xFF8BDBAA.toInt(),0xFFE87589.toInt(),0xFFF0C77D.toInt(),0xFFFFF8FF.toInt(),0xFFC8B7D2.toInt(),0xFF877890.toInt()),
        intArrayOf(0xF0010602.toInt(),0xEB031005.toInt(),0xDC061A09.toInt(),0xEB09260D.toInt(),0xFF174D22.toInt(),0xFF18A83E.toInt(),0xFF4DFF76.toInt(),0xFF82FF72.toInt(),0xFFFF5E5E.toInt(),0xFFC8FF4D.toInt(),0xFFE8FFE9.toInt(),0xFF8AC397.toInt(),0xFF4D7857.toInt()),
        intArrayOf(0xF00A0617.toInt(),0xEB140C2B.toInt(),0xDC211342.toInt(),0xEB301D5A.toInt(),0xFF563A89.toInt(),0xFF7958C8.toInt(),0xFFFFC857.toInt(),0xFF78D69B.toInt(),0xFFFF607A.toInt(),0xFFFFD77A.toInt(),0xFFFFF7E8.toInt(),0xFFBFB0D0.toInt(),0xFF796A8E.toInt()),
        intArrayOf(0xF0150817.toInt(),0xEB28102A.toInt(),0xDC3B183B.toInt(),0xEB52234F.toInt(),0xFF84406F.toInt(),0xFFFF5A91.toInt(),0xFFFF9D70.toInt(),0xFF88DF9D.toInt(),0xFFFF5656.toInt(),0xFFFFC266.toInt(),0xFFFFF1E8.toInt(),0xFFD3A3B2.toInt(),0xFF8C6575.toInt()),
        intArrayOf(0xF0090719.toInt(),0xEB130E2D.toInt(),0xDC211642.toInt(),0xEB32205B.toInt(),0xFF5E3C8A.toInt(),0xFFFF4FD8.toInt(),0xFF44E8FF.toInt(),0xFF69F3A5.toInt(),0xFFFF6F80.toInt(),0xFFFFD04A.toInt(),0xFFFFFFFF.toInt(),0xFFC0A7D4.toInt(),0xFF7B628F.toInt())
    )
    val current get() = palettes[ContestConfig.settingsTheme.coerceIn(palettes.indices)]
    val background get()=current[0]
    val surface get()=current[1]
    val card get()=current[2]
    val hover get()=current[3]
    val border get()=current[4]
    val accent get()=if(ContestConfig.settingsTheme == 4) java.awt.Color.HSBtoRGB((System.currentTimeMillis()%4000)/4000f,.66f,1f) else current[5]
    val selected get(): Int {
        val a=card;val b=accent
        fun mix(shift: Int)=(((a shr shift and 255)*.62f+(b shr shift and 255)*.38f).toInt() shl shift)
        return 0xFF000000.toInt() or mix(16) or mix(8) or mix(0)
    }
    val green get()=current[7]
    val red get()=current[8]
    val gold get()=if(ContestConfig.settingsTheme==4) accent else current[9]
    val text get()=current[10]
    val muted get()=current[11]
}
