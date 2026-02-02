import { useState, useEffect, useRef } from 'react'

export default function App() {
  const [online, setOnline] = useState(false)
  const [country, setCountry] = useState('')
  const [products, setProducts] = useState([])
  const [selectedProduct, setSelectedProduct] = useState(null)
  const [discountCode, setDiscountCode] = useState('')
  const [discountPercent, setDiscountPercent] = useState('')
  const [result, setResult] = useState(null)
  const [logs, setLogs] = useState([])
  const [copied, setCopied] = useState(null)
  const logsRef = useRef(null)

  useEffect(() => {
    fetch('/health').then(r => r.ok && setOnline(true)).catch(() => {})
  }, [])

  useEffect(() => {
    if (logsRef.current) logsRef.current.scrollTop = 0
  }, [logs])

  const addLog = (type, method, url, body, response, status, time) => {
    setLogs(prev => [{
      id: Date.now(),
      time: new Date().toLocaleTimeString(),
      type, method, url, body, response, status, duration: time
    }, ...prev].slice(0, 50))
  }

  const loadProducts = async (c) => {
    setCountry(c)
    setSelectedProduct(null)
    setResult(null)
    const start = Date.now()
    const res = await fetch(`/products?country=${c}`)
    const data = await res.json()
    addLog('GET Products', 'GET', `/products?country=${c}`, null, data, res.status, Date.now() - start)
    setProducts(res.ok ? data : [])
  }

  const applyDiscount = async () => {
    if (!selectedProduct || !discountCode || !discountPercent) {
      setResult({ ok: false, msg: 'Please fill discount code and percentage' })
      return
    }
    const start = Date.now()
    const body = { discountId: discountCode, percent: parseFloat(discountPercent) }
    const res = await fetch(`/products/${selectedProduct.id}/discount`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(body)
    })
    const data = await res.json()
    addLog('Apply Discount', 'PUT', `/products/${selectedProduct.id}/discount`, body, data, res.status, Date.now() - start)
    
    if (res.ok) {
      setResult({ ok: true, msg: data.message })
      setDiscountCode('')
      setDiscountPercent('')
      loadProducts(country)
    } else {
      setResult({ ok: false, msg: data.error || 'Failed' })
    }
  }

  const runConcurrencyTest = async () => {
    setResult(null)
    const testId = `TEST_${Date.now()}`
    const body = { discountId: testId, percent: 5 }
    const start = Date.now()
    
    const results = await Promise.all(
      Array(10).fill(0).map(() =>
        fetch('/products/laptop-se/discount', {
          method: 'PUT',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify(body)
        }).then(r => r.json())
      )
    )
    
    const applied = results.filter(r => r.message === 'Discount applied successfully').length
    const already = results.filter(r => r.message === 'Discount already applied').length
    const time = Date.now() - start
    
    addLog('Concurrency Test', 'PUT', '/products/laptop-se/discount × 10', body, 
      { applied, alreadyApplied: already, passed: applied === 1 }, 
      applied === 1 ? 200 : 500, time)
    
    setResult({ 
      ok: applied === 1, 
      msg: applied === 1 
        ? `✅ PASSED: 1 applied, ${already} rejected (${time}ms)` 
        : `❌ FAILED: ${applied} applied (should be 1)`
    })
    if (country === 'Sweden') loadProducts('Sweden')
  }

  const copyToClipboard = (text) => {
    navigator.clipboard.writeText(text)
    setCopied(text)
    setTimeout(() => setCopied(null), 1500)
  }

  const getVat = (c) => ({ Sweden: 25, Germany: 19, France: 20 }[c] || 0)

  return (
    <div style={S.page}>
      {/* HEADER */}
      <div style={S.header}>
        <div style={S.headerLeft}>
          <div style={S.logo}>⚡</div>
          <div>
            <div style={S.title}>Discount API</div>
            <div style={S.subtitle}>Electrolux • Roshan Reddy</div>
          </div>
        </div>
        <div style={{...S.status, background: online ? '#166534' : '#991b1b'}} title={online ? 'Server is running' : 'Server is offline'}>
          {online ? '● Online' : '○ Offline'}
        </div>
      </div>

      {/* MAIN SPLIT VIEW */}
      <div style={S.main}>
        {/* LEFT PANEL */}
        <div style={S.leftPanel}>
          
          {/* STEP 1: COUNTRY */}
          <div style={S.card}>
            <div style={S.cardTitle}>Step 1: Select Country</div>
            <div style={S.countries}>
              {[['Sweden', '🇸🇪', 25], ['Germany', '🇩🇪', 19], ['France', '🇫🇷', 20]].map(([name, flag, vat]) => (
                <button
                  key={name}
                  onClick={() => loadProducts(name)}
                  style={{...S.countryBtn, ...(country === name ? S.countryActive : {})}}
                  title={`Load products from ${name} (VAT rate: ${vat}%)`}
                >
                  <span style={S.flag}>{flag}</span>
                  <span style={S.countryName}>{name}</span>
                  <span style={S.vat}>VAT {vat}%</span>
                </button>
              ))}
            </div>
          </div>

          {/* STEP 2: PRODUCTS */}
          <div style={S.card}>
            <div style={S.cardTitle}>Step 2: Select Product</div>
            <div style={S.cardHint}>Click a product to select it for discount</div>
            
            {!country ? (
              <div style={S.placeholder}>👆 Select a country first</div>
            ) : products.length === 0 ? (
              <div style={S.placeholder}>Loading...</div>
            ) : (
              <div style={S.products}>
                {products.map(p => (
                  <div
                    key={p.id}
                    onClick={() => { setSelectedProduct(p); setResult(null); setDiscountCode(''); setDiscountPercent('') }}
                    style={{...S.product, ...(selectedProduct?.id === p.id ? S.productActive : {})}}
                    title="Click to select this product"
                  >
                    <div style={S.productHeader}>
                      <div>
                        <span style={S.productName}>{p.name}</span>
                        <span style={S.productId} title="Product ID used in API calls">{p.id}</span>
                      </div>
                      <button 
                        style={S.copyIdBtn} 
                        onClick={(e) => { e.stopPropagation(); copyToClipboard(p.id) }}
                        title="Copy product ID"
                      >
                        {copied === p.id ? '✓' : '📋'}
                      </button>
                    </div>
                    
                    <div style={S.priceRow}>
                      <div style={S.priceBox} title="Original price before any discounts or VAT">
                        <div style={S.priceLabel}>Base Price</div>
                        <div style={S.priceValue}>€{p.basePrice.toFixed(2)}</div>
                      </div>
                      <div style={S.arrow}>→</div>
                      <div style={S.priceBox} title={`Final price after discounts + ${getVat(p.country)}% VAT`}>
                        <div style={S.priceLabel}>Final Price</div>
                        <div style={{...S.priceValue, color: '#22c55e'}}>€{p.finalPrice.toFixed(2)}</div>
                        <div style={S.vatNote}>incl. {getVat(p.country)}% VAT</div>
                      </div>
                    </div>

                    {p.discounts?.length > 0 && (
                      <div style={S.discountsSection}>
                        <div style={S.discountsHeader} title="These discount codes have been applied to this product">
                          Applied Discounts ({p.discounts.length}):
                        </div>
                        <div style={S.discountsList}>
                          {p.discounts.map((d, i) => (
                            <div 
                              key={i} 
                              style={S.discountChip}
                              title={`Click to copy "${d.discountId}" (${d.percent}% off)`}
                              onClick={(e) => { 
                                e.stopPropagation()
                                copyToClipboard(d.discountId)
                              }}
                            >
                              <span style={S.discountCode}>{d.discountId}</span>
                              <span style={S.discountPct}>-{d.percent}%</span>
                              <span style={S.copyIcon}>{copied === d.discountId ? '✓' : '📋'}</span>
                            </div>
                          ))}
                        </div>
                      </div>
                    )}
                  </div>
                ))}
              </div>
            )}
          </div>

          {/* STEP 3: APPLY DISCOUNT */}
          <div style={S.card}>
            <div style={S.cardTitle}>Step 3: Apply New Discount</div>
            
            {!selectedProduct ? (
              <div style={S.placeholder}>👆 Select a product first</div>
            ) : (
              <>
                <div style={S.selectedInfo} title="The product you selected in Step 2">
                  Applying to: <strong>{selectedProduct.name}</strong> <span style={S.selectedId}>({selectedProduct.id})</span>
                </div>
                
                <div style={S.formRow}>
                  <div style={S.formField}>
                    <label style={S.label} title="A unique code for this discount (e.g., SUMMER24, FLASH_SALE)">
                      Discount Code *
                    </label>
                    <input
                      style={S.input}
                      placeholder="e.g., SUMMER24"
                      value={discountCode}
                      onChange={e => setDiscountCode(e.target.value.toUpperCase())}
                      title="Enter a unique discount code"
                    />
                  </div>
                  
                  <div style={{...S.formField, width: '120px'}}>
                    <label style={S.label} title="How much percent off (0.01 to 100)">
                      Percentage *
                    </label>
                    <div style={S.percentInput}>
                      <input
                        style={{...S.input, paddingRight: '30px'}}
                        type="number"
                        min="0.01"
                        max="100"
                        step="0.01"
                        placeholder="15"
                        value={discountPercent}
                        onChange={e => setDiscountPercent(e.target.value)}
                        title="Enter discount percentage (e.g., 15 for 15% off)"
                      />
                      <span style={S.percentSymbol}>%</span>
                    </div>
                  </div>
                  
                  <button 
                    style={S.applyBtn} 
                    onClick={applyDiscount}
                    title="Send PUT request to apply this discount"
                  >
                    Apply
                  </button>
                </div>
                
                <div style={S.quickFill}>
                  <span style={S.quickLabel} title="Click to auto-fill the form with test values">Quick fill:</span>
                  {[['SUMMER24', 15], ['FLASH_SALE', 10], ['VIP_DEAL', 25]].map(([code, pct]) => (
                    <button
                      key={code}
                      style={S.quickBtn}
                      onClick={() => { setDiscountCode(code); setDiscountPercent(String(pct)) }}
                      title={`Fill form with ${code} at ${pct}% off`}
                    >
                      {code} ({pct}%)
                    </button>
                  ))}
                </div>

                {result && (
                  <div style={{...S.result, background: result.ok ? '#14532d' : '#7f1d1d'}}>
                    {result.msg}
                  </div>
                )}
              </>
            )}
          </div>

          {/* CONCURRENCY TEST */}
          <div style={S.card}>
            <div style={S.cardTitle} title="Test that the same discount can only be applied once">🧪 Concurrency Test</div>
            <p style={S.testDesc}>
              Sends 10 parallel requests with the same discount code to test idempotency.
              <br/><strong>Expected:</strong> Only 1 succeeds, 9 return "already applied".
            </p>
            <button style={S.testBtn} onClick={runConcurrencyTest} title="Run 10 parallel PUT requests to laptop-se">
              Run Test (10 parallel requests)
            </button>
            {result && result.msg.includes('PASSED') && (
              <div style={{...S.result, background: '#14532d', marginTop: '12px'}}>{result.msg}</div>
            )}
            {result && result.msg.includes('FAILED') && (
              <div style={{...S.result, background: '#7f1d1d', marginTop: '12px'}}>{result.msg}</div>
            )}
          </div>
        </div>

        {/* RIGHT PANEL - LOGS */}
        <div style={S.rightPanel}>
          <div style={S.logsHeader}>
            <span style={S.logsTitle} title="All API requests and responses are logged here">📋 API Logs</span>
            <button style={S.clearBtn} onClick={() => setLogs([])} title="Clear all logs">Clear</button>
          </div>
          
          <div style={S.logsContainer} ref={logsRef}>
            {logs.length === 0 ? (
              <div style={S.logsEmpty}>
                API requests will appear here.<br/><br/>
                Try selecting a country!
              </div>
            ) : (
              logs.map(log => (
                <div key={log.id} style={S.logItem}>
                  <div style={S.logTop}>
                    <span 
                      style={{...S.logMethod, background: log.method === 'GET' ? '#2563eb' : '#7c3aed'}}
                      title={log.method === 'GET' ? 'HTTP GET request' : 'HTTP PUT request'}
                    >
                      {log.method}
                    </span>
                    <span style={S.logType}>{log.type}</span>
                    <span 
                      style={{...S.logStatus, color: log.status < 300 ? '#22c55e' : '#ef4444'}}
                      title={`HTTP status code: ${log.status}`}
                    >
                      {log.status}
                    </span>
                    <span style={S.logDuration} title="Request duration">{log.duration}ms</span>
                  </div>
                  <div style={S.logUrl} title="API endpoint URL">{log.url}</div>
                  
                  {log.body && (
                    <>
                      <div style={S.logLabel}>REQUEST BODY:</div>
                      <pre style={S.logPre}>{JSON.stringify(log.body, null, 2)}</pre>
                    </>
                  )}
                  
                  <div style={S.logLabel}>RESPONSE:</div>
                  <pre style={{...S.logPre, borderLeft: `3px solid ${log.status < 300 ? '#22c55e' : '#ef4444'}`}}>
                    {JSON.stringify(log.response, null, 2)}
                  </pre>
                </div>
              ))
            )}
          </div>
        </div>
      </div>

      {/* FOOTER */}
      <div style={S.footer}>
        Built by <strong>Roshan Reddy</strong> • Kotlin + Ktor + PostgreSQL + React
      </div>
    </div>
  )
}

const S = {
  page: { height: '100vh', display: 'flex', flexDirection: 'column', background: '#0f172a', color: '#e2e8f0', fontFamily: 'system-ui, sans-serif', overflow: 'hidden' },
  
  header: { display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '12px 20px', borderBottom: '1px solid #334155', flexShrink: 0 },
  headerLeft: { display: 'flex', alignItems: 'center', gap: '10px' },
  logo: { fontSize: '24px' },
  title: { fontSize: '18px', fontWeight: '600' },
  subtitle: { fontSize: '12px', color: '#94a3b8' },
  status: { padding: '5px 12px', borderRadius: '16px', fontSize: '12px', fontWeight: '500', cursor: 'help' },
  
  main: { flex: 1, display: 'flex', overflow: 'hidden' },
  leftPanel: { flex: 1, padding: '16px', overflowY: 'auto', borderRight: '1px solid #334155' },
  rightPanel: { width: '420px', display: 'flex', flexDirection: 'column', background: '#0a0f1a', flexShrink: 0 },
  
  card: { background: '#1e293b', borderRadius: '10px', padding: '16px', marginBottom: '16px' },
  cardTitle: { fontSize: '14px', fontWeight: '600', marginBottom: '4px', color: '#f1f5f9' },
  cardHint: { fontSize: '11px', color: '#64748b', marginBottom: '12px' },
  
  countries: { display: 'flex', gap: '10px' },
  countryBtn: { flex: 1, display: 'flex', flexDirection: 'column', alignItems: 'center', padding: '12px', background: '#334155', border: '2px solid transparent', borderRadius: '8px', cursor: 'pointer', color: '#e2e8f0', transition: 'all 0.2s' },
  countryActive: { borderColor: '#3b82f6', background: '#1e3a5f' },
  flag: { fontSize: '24px', marginBottom: '4px' },
  countryName: { fontWeight: '600', fontSize: '13px' },
  vat: { fontSize: '11px', color: '#94a3b8' },
  
  placeholder: { textAlign: 'center', padding: '24px', color: '#64748b', fontSize: '14px' },
  
  products: { display: 'flex', flexDirection: 'column', gap: '10px' },
  product: { padding: '12px', background: '#334155', borderRadius: '8px', cursor: 'pointer', border: '2px solid transparent', transition: 'all 0.2s' },
  productActive: { borderColor: '#3b82f6', background: '#1e3a5f' },
  productHeader: { display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '10px' },
  productName: { fontWeight: '600', fontSize: '14px', marginRight: '8px' },
  productId: { fontSize: '11px', color: '#94a3b8', fontFamily: 'monospace', background: '#1e293b', padding: '2px 6px', borderRadius: '4px', cursor: 'help' },
  copyIdBtn: { padding: '4px 8px', background: '#475569', border: 'none', borderRadius: '4px', cursor: 'pointer', fontSize: '12px' },
  
  priceRow: { display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '10px' },
  priceBox: { flex: 1, background: '#1e293b', padding: '8px 10px', borderRadius: '6px', textAlign: 'center', cursor: 'help' },
  priceLabel: { fontSize: '10px', color: '#94a3b8', marginBottom: '2px' },
  priceValue: { fontSize: '16px', fontWeight: '700' },
  vatNote: { fontSize: '9px', color: '#64748b', marginTop: '2px' },
  arrow: { color: '#64748b' },
  
  discountsSection: { marginTop: '10px', padding: '10px', background: '#1e293b', borderRadius: '6px' },
  discountsHeader: { fontSize: '11px', color: '#94a3b8', marginBottom: '8px', cursor: 'help' },
  discountsList: { display: 'flex', flexWrap: 'wrap', gap: '6px' },
  discountChip: { display: 'flex', alignItems: 'center', gap: '6px', padding: '4px 8px', background: '#854d0e', borderRadius: '4px', cursor: 'pointer', transition: 'all 0.2s' },
  discountCode: { fontFamily: 'monospace', fontSize: '11px', color: '#fef08a' },
  discountPct: { fontSize: '11px', color: '#fde047', fontWeight: '600' },
  copyIcon: { fontSize: '10px', opacity: 0.7 },
  
  selectedInfo: { fontSize: '13px', color: '#94a3b8', marginBottom: '14px', padding: '10px', background: '#334155', borderRadius: '6px' },
  selectedId: { fontFamily: 'monospace', color: '#64748b' },
  
  formRow: { display: 'flex', gap: '10px', alignItems: 'flex-end', marginBottom: '12px' },
  formField: { flex: 1 },
  label: { display: 'block', fontSize: '11px', color: '#94a3b8', marginBottom: '4px', cursor: 'help' },
  input: { width: '100%', padding: '8px 12px', background: '#334155', border: '1px solid #475569', borderRadius: '6px', color: '#e2e8f0', fontSize: '13px', outline: 'none', boxSizing: 'border-box' },
  percentInput: { position: 'relative' },
  percentSymbol: { position: 'absolute', right: '10px', top: '50%', transform: 'translateY(-50%)', color: '#64748b', fontSize: '13px' },
  applyBtn: { padding: '8px 20px', background: '#3b82f6', border: 'none', borderRadius: '6px', color: 'white', fontWeight: '600', cursor: 'pointer', fontSize: '13px', height: '36px' },
  
  quickFill: { display: 'flex', alignItems: 'center', gap: '8px', flexWrap: 'wrap' },
  quickLabel: { fontSize: '11px', color: '#64748b', cursor: 'help' },
  quickBtn: { padding: '4px 8px', background: '#475569', border: 'none', borderRadius: '4px', color: '#e2e8f0', fontSize: '11px', cursor: 'pointer' },
  
  result: { marginTop: '12px', padding: '10px 12px', borderRadius: '6px', fontSize: '13px' },
  
  testDesc: { fontSize: '12px', color: '#94a3b8', marginBottom: '12px', lineHeight: '1.5' },
  testBtn: { padding: '10px 16px', background: '#7c3aed', border: 'none', borderRadius: '6px', color: 'white', fontWeight: '600', cursor: 'pointer', fontSize: '13px', width: '100%' },
  
  logsHeader: { display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '12px 16px', borderBottom: '1px solid #334155' },
  logsTitle: { fontWeight: '600', fontSize: '14px', cursor: 'help' },
  clearBtn: { padding: '4px 10px', background: '#334155', border: 'none', borderRadius: '4px', color: '#94a3b8', fontSize: '11px', cursor: 'pointer' },
  logsContainer: { flex: 1, overflowY: 'auto', padding: '12px' },
  logsEmpty: { textAlign: 'center', padding: '40px 20px', color: '#64748b', fontSize: '13px', lineHeight: '1.6' },
  
  logItem: { background: '#1e293b', borderRadius: '8px', padding: '12px', marginBottom: '12px' },
  logTop: { display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '6px' },
  logMethod: { padding: '2px 6px', borderRadius: '4px', fontSize: '10px', fontWeight: '700', color: 'white', cursor: 'help' },
  logType: { fontWeight: '600', fontSize: '12px', flex: 1 },
  logStatus: { fontSize: '12px', fontWeight: '600', cursor: 'help' },
  logDuration: { fontSize: '10px', color: '#64748b', background: '#334155', padding: '2px 6px', borderRadius: '4px', cursor: 'help' },
  logUrl: { fontSize: '11px', color: '#94a3b8', fontFamily: 'monospace', marginBottom: '8px', cursor: 'help' },
  logLabel: { fontSize: '9px', fontWeight: '700', color: '#64748b', marginBottom: '4px', marginTop: '8px' },
  logPre: { margin: 0, padding: '8px', background: '#0f172a', borderRadius: '4px', fontSize: '10px', fontFamily: 'monospace', color: '#a5f3fc', overflow: 'auto', maxHeight: '150px', whiteSpace: 'pre-wrap' },
  
  footer: { textAlign: 'center', padding: '10px', fontSize: '11px', color: '#64748b', borderTop: '1px solid #334155', flexShrink: 0 }
}
