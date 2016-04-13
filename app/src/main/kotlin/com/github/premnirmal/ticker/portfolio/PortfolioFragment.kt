package com.github.premnirmal.ticker.portfolio

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Parcelable
import android.support.design.widget.CoordinatorLayout
import android.support.v7.widget.GridLayoutManager
import android.view.*
import com.daimajia.swipe.SwipeLayout
import com.github.premnirmal.ticker.*
import com.github.premnirmal.ticker.events.NoNetworkEvent
import com.github.premnirmal.ticker.events.StockUpdatedEvent
import com.github.premnirmal.ticker.events.UpdateFailedEvent
import com.github.premnirmal.ticker.model.IStocksProvider
import com.github.premnirmal.ticker.network.Stock
import com.github.premnirmal.ticker.portfolio.drag_drop.RearrangeActivity
import com.github.premnirmal.ticker.settings.SettingsActivity
import com.github.premnirmal.ticker.ui.SpacingDecoration
import com.github.premnirmal.tickerwidget.R
import kotlinx.android.synthetic.main.portfolio_fragment.*
import rx.android.schedulers.AndroidSchedulers
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * Created by premnirmal on 2/25/16.
 */
class PortfolioFragment : BaseFragment() {

  companion object {
    private val LIST_INSTANCE_STATE = "LIST_INSTANCE_STATE"
    private val NO_NETWORK_THROTTLE_INTERVAL = 1000L
  }

  @Inject
  lateinit internal var stocksProvider: IStocksProvider

  @Inject
  lateinit internal var bus: RxBus

  private val handler = Handler(Looper.getMainLooper())
  private var listViewState: Parcelable? = null
  private val stocksAdapter by lazy {
    StocksAdapter(stocksProvider,
        object : StocksAdapter.OnStockClickListener {
          override fun onRemoveClick(view: View, stock: Stock?, position: Int) {
            promptRemove(stock, position)
          }

          override fun onClick(stock: Stock?) {
            AlertDialog.Builder(activity).setItems(R.array.graph_or_positions,
                { dialog, which ->
                  val intent: Intent
                  if (which == 0) {
                    intent = Intent(activity, GraphActivity::class.java)
                    intent.putExtra(GraphActivity.GRAPH_DATA, stock)
                  } else {
                    intent = Intent(activity, EditPositionActivity::class.java)
                    intent.putExtra(EditPositionActivity.TICKER, stock?.symbol)
                  }
                  activity.startActivity(intent)
                }).show()
          }
        })
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    Injector.getAppComponent().inject(this)
    setHasOptionsMenu(true)
  }

  override fun onResume() {
    super.onResume()
    update()
    if (listViewState != null) {
      stockList?.layoutManager?.onRestoreInstanceState(listViewState)
    }
  }

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
      savedInstanceState: Bundle?): View? {
    val view = inflater.inflate(R.layout.portfolio_fragment, null)
    return view
  }

  override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    stockList.addItemDecoration(
        SpacingDecoration(context.resources.getDimensionPixelSize(R.dimen.list_spacing)))
    stockList.layoutManager = GridLayoutManager(context, 2)
    swipe_container.setColorSchemeResources(R.color.color_secondary, R.color.spicy_salmon,
        R.color.sea)
    swipe_container.setOnRefreshListener({
      stocksProvider.fetch()
    })
    fragment_root.invalidate()
    bind(bus.forEventType(NoNetworkEvent::class.java))
        .throttleLast(NO_NETWORK_THROTTLE_INTERVAL, TimeUnit.MILLISECONDS)
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe { event ->
          noNetwork(event as NoNetworkEvent)
          swipe_container.isRefreshing = false
        }

    bind(bus.forEventType(StockUpdatedEvent::class.java)).subscribe { event ->
      update()
      swipe_container.isRefreshing = false
    }

    bind(bus.forEventType(UpdateFailedEvent::class.java)).subscribe { event ->
      swipe_container.isRefreshing = false
      InAppMessage.showMessage(fragment_root, getString(R.string.refresh_failed))
    }

    if (!Tools.isNetworkOnline(context.applicationContext)) {
      noNetwork(NoNetworkEvent())
    }
    if (savedInstanceState != null) {
      listViewState = savedInstanceState.getParcelable<Parcelable>(LIST_INSTANCE_STATE)
    }

    val params = add_ticker_button.layoutParams as CoordinatorLayout.LayoutParams
    params.behavior = FABBehaviour()
    add_ticker_button.layoutParams = params
    add_ticker_button.setOnClickListener({ v ->
      val intent = Intent(v.context, TickerSelectorActivity::class.java)
      startActivity(intent)
    })
  }

  private fun update() {
    val activity = activity
    if (activity != null) {
      if (stocksProvider.getStocks().isEmpty()) {
        handler.postDelayed({ update() }, 600)
      }

      last_updated?.text = "Last fetch: ${stocksProvider.lastFetched()}"
      next_update?.text = "Next fetch: ${stocksProvider.nextFetch()}"

      if (stockList != null) {
        stocksAdapter.refresh(stocksProvider)
        stockList.adapter = stocksAdapter
        if (stocksAdapter.itemCount > 1) {
          if (Tools.firstTimeViewingSwipeLayout()) {
            handler.postDelayed({ // TODO think of a better way to do this.. nesting is ugly
              if (isVisible) {
                val layout = stockList?.getChildAt(0) as SwipeLayout?
                if (layout != null) {
                  layout.open(true)
                  handler.postDelayed({
                    if (isVisible) {
                      layout.close()
                      val secondLayout = stockList?.getChildAt(1) as SwipeLayout?
                      if (isVisible) {
                        if (secondLayout != null) {
                          secondLayout.open(true)
                          handler.postDelayed({
                            if (isVisible) {
                              secondLayout.close()
                            }
                          }, 600)
                        }
                      }
                    }
                  }, 600)
                }
              }
            }, 1000)
          }
        }
      }
    }
  }

  private fun promptRemove(stock: Stock?, position: Int) {
    if (stock != null) {
      AlertDialog.Builder(activity).setTitle("Remove")
          .setMessage("Are you sure you want to remove ${stock.symbol} from your portfolio?")
          .setPositiveButton("Remove", { dialog, which ->
            stocksProvider.removeStock(stock.symbol)
            if (stocksAdapter.remove(stock)) {
              stocksAdapter.notifyItemRemoved(position)
            }
            dialog.dismiss()
          })
          .setNegativeButton("Cancel", { dialog, which -> dialog.dismiss() }).show()
    }
  }

  override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
    super.onCreateOptionsMenu(menu, inflater)
    menu.clear()
    inflater.inflate(R.menu.menu_paranormal, menu)
    val rearrangeItem = menu.findItem(R.id.action_rearrange)
    rearrangeItem.isEnabled = !Tools.autoSortEnabled()
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    val activity = activity
    val itemId = item.itemId
    if (itemId == R.id.action_settings) {
      val intent = Intent(activity, SettingsActivity::class.java)
      startActivity(intent)
      return true
    } else if (itemId == R.id.action_rearrange) {
      startActivity(Intent(activity, RearrangeActivity::class.java))
      return true
    }
    return super.onOptionsItemSelected(item)
  }

  private fun noNetwork(event: NoNetworkEvent) {
    InAppMessage.showMessage(fragment_root, getString(R.string.no_network_message))
  }

  override fun onSaveInstanceState(outState: Bundle?) {
    super.onSaveInstanceState(outState)
    listViewState = stockList?.layoutManager?.onSaveInstanceState()
    if (listViewState != null) {
      outState?.putParcelable(LIST_INSTANCE_STATE, listViewState)
    }
  }
}