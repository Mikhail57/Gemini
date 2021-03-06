package com.haroncode.gemini.sample.base

import android.os.Bundle
import androidx.annotation.LayoutRes
import androidx.fragment.app.Fragment
import com.haroncode.gemini.sample.di.DI
import com.haroncode.gemini.sample.di.scope.PerFragment
import com.haroncode.gemini.sample.util.objectScopeName
import timber.log.Timber
import toothpick.Scope
import toothpick.ktp.KTP

abstract class BaseFragment constructor(
    @LayoutRes private val layoutRes: Int = 0
) : Fragment(layoutRes) {

    private var instanceStateSaved: Boolean = false

    protected open val parentScopeName: String by lazy {
        (parentFragment as? BaseFragment)?.fragmentScopeName
            ?: DI.APP_SCOPE
    }

    protected open val scopeModuleInstaller: (Scope) -> Unit = {}

    private lateinit var fragmentScopeName: String
    protected lateinit var scope: Scope
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
        fragmentScopeName = savedInstanceState?.getString(STATE_SCOPE_NAME) ?: objectScopeName()

        if (KTP.isScopeOpen(fragmentScopeName)) {
            Timber.d("Get exist UI scope: $fragmentScopeName")
            scope = KTP.openScope(fragmentScopeName)
        } else {
            Timber.d("Init new UI scope: $fragmentScopeName")
            scope = KTP.openScopes(parentScopeName, fragmentScopeName)
            scopeModuleInstaller.invoke(scope)
        }
        scope.supportScopeAnnotation(PerFragment::class.java)
        scope.inject(this)

        super.onCreate(savedInstanceState)
    }

    override fun onResume() {
        super.onResume()
        instanceStateSaved = false
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        instanceStateSaved = true
        outState.putString(STATE_SCOPE_NAME, fragmentScopeName)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (needCloseScope()) {
            // destroy this fragment with scope
            Timber.d("Destroy UI scope: $fragmentScopeName")
            KTP.closeScope(scope.name)
        }
    }

    // This is android, baby!
    private fun isRealRemoving(): Boolean =
        (isRemoving && !instanceStateSaved) || // because isRemoving == true for fragment in backstack on screen rotation
            ((parentFragment as? BaseFragment)?.isRealRemoving() ?: false)

    // It will be valid only for 'onDestroy()' method
    private fun needCloseScope(): Boolean =
        when {
            activity?.isChangingConfigurations == true -> false
            activity?.isFinishing == true -> true
            else -> isRealRemoving()
        }

    open fun onBackPressed() {}

    companion object {
        private const val STATE_SCOPE_NAME = "state_scope_name"
    }
}
