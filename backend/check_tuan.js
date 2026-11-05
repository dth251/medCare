async function checkDbTuan() {
  const loginDoc = await fetch('http://localhost:8080/api/auth/login', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username: 'ql.levanc', password: '123456' })
  }).then(r => r.json());

  const token = loginDoc.data.token;
  for (let i = 1; i <= 5; i++) {
    const res = await fetch('http://localhost:8080/api/phan-cong/tuan/' + i, {
      headers: { 'Authorization': 'Bearer ' + token }
    }).then(r => r.json()).catch(() => null);
    if (res && res.data && res.data.length > 0) {
      console.log('Tuan ID ' + i + ':', res.data[0].tuanLamViec.ten, 'TrangThai:', res.data[0].tuanLamViec.trangThai);
    }
  }
}
checkDbTuan();
