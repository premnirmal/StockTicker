package com.github.premnirmal.ticker.portfolio.drag_drop

import android.content.SharedPreferences
import android.os.Bundle
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.helper.ItemTouchHelper
import com.github.premnirmal.ticker.BaseActivity
import com.github.premnirmal.ticker.Injector
import com.github.premnirmal.ticker.Tools
import com.github.premnirmal.ticker.model.IStocksProvider
import com.github.premnirmal.ticker.ui.SpacingDecoration
import com.github.premnirmal.tickerwidget.R
import kotlinx.android.synthetic.main.activity_rearrange.*
import javax.inject.Inject

/**
 * Created by premnirmal on 2/29/16.
 */
class RearrangeActivity : BaseActivity(), OnStartDragListener {

  private var itemTouchHelper: ItemTouchHelper? = null

  @Inject
  lateinit internal var stocksProvider: IStocksProvider

  @Inject
  lateinit internal var preferences: SharedPreferences

  public override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    Injector.inject(this)
    setContentView(R.layout.activity_rearrange)
    updateToolbar(toolbar)
    toolbar.setNavigationOnClickListener {
      finish()
    }
    recyclerView.setHasFixedSize(true)
    val spacing = resources.getDimensionPixelSize(R.dimen.list_spacing)
    recyclerView.setPadding(spacing, spacing, spacing, 0)
    recyclerView.addItemDecoration(SpacingDecoration(spacing))
    val adapter = RearrangeAdapter(stocksProvider, this)
    recyclerView.adapter = adapter
    val layoutManager: RecyclerView.LayoutManager = GridLayoutManager(this, 2)
    recyclerView.layoutManager = layoutManager

    val callback: ItemTouchHelper.Callback = SimpleItemTouchHelperCallback(adapter)
    itemTouchHelper = ItemTouchHelper(callback)
    itemTouchHelper?.attachToRecyclerView(recyclerView)
    if (Tools.autoSortEnabled()) {
      preferences.edit().putBoolean(Tools.SETTING_AUTOSORT, false).apply()
      showDialog(getString(R.string.autosort_disabled))
    }
  }

  override fun onStartDrag(viewHolder: RecyclerView.ViewHolder) {
    itemTouchHelper?.startDrag(viewHolder)
  }
}