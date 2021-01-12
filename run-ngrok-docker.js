#!/bin/node

const os = require('os');
const { execSync } = require('child_process');

const findIp = () => {
    const interfaces = os.networkInterfaces();
    let ipAddress;
    Object.keys(interfaces).some(name => {
        const interface = interfaces[name];
        const IpObj = interface.find(ip => ip.internal === false && 'IPv4' === ip.family);
        if(IpObj) {
            ipAddress = IpObj.address;
            return true;
        }
    })
    return ipAddress;
}

const retry = (fn, retries=0, ms=1000, maxRetries=30) => {
    try {
        fn();
    } catch(err) {
        setTimeout(() => {
            ++retries;
            if(retries===maxRetries) {
                throw Error('maximum retries exceeded');
            }
            retry(fn, retries);
        }, ms);
    }
}

const displayNgrokUrl = () => {
    let content = execSync('docker exec -i ngrok curl localhost:4040/api/tunnels', { stdio: 'pipe' }).toString().trim();
    content = JSON.parse(content);
    const { public_url } = content.tunnels[0];
    console.log(public_url);
}

const port = process.argv[2] || 8080;
const ip = findIp();

try {
    execSync('docker rm ngrok -f', { stdio: 'ignore' });
} catch(err) {}

execSync(`docker run -d --name ngrok wernight/ngrok ngrok http ${ip}:${port}`);

console.log(`proxying for ${ip}:${port}. make sure process is listening on host 0.0.0.0 and port ${port}.`);
console.log(`localhost is not the same as 0.0.0.0`)
console.log('generating public url...')

retry(displayNgrokUrl);