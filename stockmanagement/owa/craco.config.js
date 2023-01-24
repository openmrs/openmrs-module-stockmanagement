const path = require('path');
const ReplaceInFileWebpackPlugin = require('replace-in-file-webpack-plugin');

module.exports = {    
  webpack: {
    alias: {
      '@': path.resolve(__dirname, 'src'),
        },
        plugins: [
            new ReplaceInFileWebpackPlugin([{
                dir:  path.resolve('../omod/src/main/webapp/resources/spa', 'static/js'),
                test: /\.js/,
                rules: [
                    {
                        search: /\"\/%STOCKMGMT_BASE_URL%\//g,
                        replace: 'window.STOCKMGMT_RESOURCE_URL + "'
                    },]
            }, {
                dir: path.resolve('../omod/src/main/webapp/resources/spa', 'static/css'),
                test: [/\.css$/],
                rules: [
                    {
                        search: /\/%STOCKMGMT_BASE_URL%/g,
                        replace: '../..'
                    }
                ]
            }])
        ],
    },
    
};
