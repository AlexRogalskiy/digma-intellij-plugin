package org.digma.intellij.plugin.toolwindow.recentactivity

import org.cef.browser.CefBrowser
import org.cef.browser.CefFrame
import org.cef.callback.CefSchemeHandlerFactory
import org.cef.handler.CefResourceHandler
import org.cef.network.CefRequest

class CustomSchemeHandlerFactory: CefSchemeHandlerFactory {
    override fun create(browser: CefBrowser?, frame: CefFrame?, schemeName: String?, request: CefRequest?): CefResourceHandler {
        return CustomResourceHandler()
    }
}