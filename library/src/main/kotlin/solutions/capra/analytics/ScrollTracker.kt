package solutions.capra.analytics

import android.view.View
import android.view.ViewTreeObserver
import androidx.core.widget.NestedScrollView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.widget.ScrollView

/**
 * Tracks scroll depth for various scrollable views
 */
class ScrollTracker {
    private var maxScrollPercent: Int = 0
    private var currentView: View? = null
    private var scrollListener: Any? = null

    /**
     * Current maximum scroll depth reached (0-100)
     */
    val maxScrollDepth: Int
        get() = maxScrollPercent

    /**
     * Attach to a RecyclerView to track scroll depth
     */
    fun attachTo(recyclerView: RecyclerView) {
        detach()
        currentView = recyclerView

        val listener = object : RecyclerView.OnScrollListener() {
            override fun onScrolled(rv: RecyclerView, dx: Int, dy: Int) {
                updateScrollDepth(calculateRecyclerViewScrollPercent(rv))
            }
        }

        recyclerView.addOnScrollListener(listener)
        scrollListener = listener
    }

    /**
     * Attach to a NestedScrollView to track scroll depth
     */
    fun attachTo(nestedScrollView: NestedScrollView) {
        detach()
        currentView = nestedScrollView

        val listener = NestedScrollView.OnScrollChangeListener { v, _, scrollY, _, _ ->
            val maxScroll = v.getChildAt(0).height - v.height
            if (maxScroll > 0) {
                val percent = (scrollY * 100) / maxScroll
                updateScrollDepth(percent.coerceIn(0, 100))
            }
        }

        nestedScrollView.setOnScrollChangeListener(listener)
        scrollListener = listener
    }

    /**
     * Attach to a ScrollView to track scroll depth
     */
    fun attachTo(scrollView: ScrollView) {
        detach()
        currentView = scrollView

        val listener = ViewTreeObserver.OnScrollChangedListener {
            val maxScroll = scrollView.getChildAt(0).height - scrollView.height
            if (maxScroll > 0) {
                val percent = (scrollView.scrollY * 100) / maxScroll
                updateScrollDepth(percent.coerceIn(0, 100))
            }
        }

        scrollView.viewTreeObserver.addOnScrollChangedListener(listener)
        scrollListener = listener
    }

    /**
     * Manually report scroll depth (for custom scroll implementations)
     * @param percent Scroll percentage (0-100)
     */
    fun reportScrollDepth(percent: Int) {
        updateScrollDepth(percent.coerceIn(0, 100))
    }

    /**
     * Reset scroll tracking (call when navigating to a new screen)
     */
    fun reset() {
        maxScrollPercent = 0
    }

    /**
     * Detach from current view
     */
    fun detach() {
        when (val view = currentView) {
            is RecyclerView -> {
                (scrollListener as? RecyclerView.OnScrollListener)?.let {
                    view.removeOnScrollListener(it)
                }
            }
            is ScrollView -> {
                (scrollListener as? ViewTreeObserver.OnScrollChangedListener)?.let {
                    view.viewTreeObserver.removeOnScrollChangedListener(it)
                }
            }
            // NestedScrollView listener can't be removed, but setting null view handles it
        }

        currentView = null
        scrollListener = null
    }

    private fun updateScrollDepth(percent: Int) {
        if (percent > maxScrollPercent) {
            maxScrollPercent = percent
        }
    }

    private fun calculateRecyclerViewScrollPercent(recyclerView: RecyclerView): Int {
        val layoutManager = recyclerView.layoutManager as? LinearLayoutManager ?: return 0

        val totalItemCount = layoutManager.itemCount
        if (totalItemCount == 0) return 0

        val lastVisiblePosition = layoutManager.findLastVisibleItemPosition()
        val firstVisiblePosition = layoutManager.findFirstVisibleItemPosition()

        if (lastVisiblePosition == RecyclerView.NO_POSITION) return 0

        // Calculate based on visible items
        val visibleItems = lastVisiblePosition - firstVisiblePosition + 1
        val scrollableItems = totalItemCount - visibleItems

        if (scrollableItems <= 0) return 100

        val scrolledItems = firstVisiblePosition
        return ((scrolledItems * 100) / scrollableItems).coerceIn(0, 100)
    }
}
