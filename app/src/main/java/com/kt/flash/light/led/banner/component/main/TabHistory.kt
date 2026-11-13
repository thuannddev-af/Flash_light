package com.kt.flash.light.led.banner.component.main

/** ViewPager pages have a separate history from FragmentManager transactions. */
internal class TabHistory(restoredPages: List<Int> = emptyList()) {
    private val pages = mutableListOf(MainPagerAdapter.FLASH_PAGE)
    val current: Int get() = pages.last()

    init {
        restoredPages.filter { it in 0 until MainPagerAdapter.PAGE_COUNT }.forEach(::visit)
    }

    fun visit(page: Int) {
        require(page in 0 until MainPagerAdapter.PAGE_COUNT)
        if (page == MainPagerAdapter.FLASH_PAGE) {
            pages.clear()
            pages.add(page)
        } else if (page != current) {
            pages.add(page)
        }
    }

    fun previous(): Int? {
        if (current == MainPagerAdapter.FLASH_PAGE) return null
        pages.removeAt(pages.lastIndex)
        return current
    }

    fun snapshot(): IntArray = pages.toIntArray()
}
