
var jsConnector = {
    showResult: function (result) {
        //console.log(result);

        var data = JSON.parse(result)
        var testData = data.testData;

        //testChart(testData);
        updateChartMS1(data);
        updateChartMS2(data);
        updateChartMSN(data);
    }
};

function testChart(result) {
    Highcharts.chart('container', {
        chart: {
            type: 'bar'
        },
        title: {
            text: 'Historic World Population by Region'
        },
        subtitle: {
            text: 'Source: <a href="https://en.wikipedia.org/wiki/World_population">Wikipedia.org</a>'
        },
        xAxis: {
            categories: ['Africa', 'America', 'Asia', 'Europe', 'Oceania'],
            title: {
                text: null
            }
        },
        yAxis: {
            min: 0,
            title: {
                text: 'Population (millions)',
                align: 'high'
            },
            labels: {
                overflow: 'justify'
            }
        },
        tooltip: {
            valueSuffix: ' millions'
        },
        plotOptions: {
            bar: {
                dataLabels: {
                    enabled: true
                }
            }
        },
        legend: {
            layout: 'vertical',
            align: 'right',
            verticalAlign: 'top',
            x: -40,
            y: 80,
            floating: true,
            borderWidth: 1,
            backgroundColor:
                    Highcharts.defaultOptions.legend.backgroundColor || '#FFFFFF',
            shadow: true
        },
        credits: {
            enabled: false
        },
        series: result
    });
}

function updateChartMS1(data) {
    Highcharts.chart('ms1chart', {
        chart: {
            type: 'column',
            zoomType: 'xy'
        },
        title: {
            text: data.ms1Data.titleMs1
        },
        subtitle: {
            text: data.ms1Data.subtitleMs1
        },
        xAxis: {
            min: data.ms1Data.minMZ_ms1,
            max: data.ms1Data.maxMZ_ms1,
            allowDecimals: true,
            title: {
                text: 'M/Z'
            },
            labels: {
                formatter: function () {
                    return this.value;
                }
            },
            plotBands: data.ms1Data.scoreBands
        },
        yAxis: {
            min: 0,
            title: {
                text: 'Intensity'
            },
            labels: {
                formatter: function () {
                    return this.value / 1000 + 'k';
                }
            }
        },
        /*exporting: {
            filename: 'MS1_' + data.ms1Data.searchID + '_' + data.ms1Data.peptideID,
            sourceWidth: 1280,
            sourceHeight: 720
        },*/
        credits: {
            enabled: false
        },
        legend: {
            enabled: true,
            borderWidth: 1,
            backgroundColor: (Highcharts.theme && Highcharts.theme.legendBackgroundColor) || '#FFFFFF'
        },
        tooltip: {
            formatter: function () {
                if (this.y === 0)
                    return false;
                else {
                    var pow = Math.pow(10, 3);
                    return '<b>MZ:</b> ' + Math.round(this.x * pow) / pow + '<br/><b>Intensity:</b> ' + Math.round(this.y);
                }
            }
        },
        plotOptions: {
            series: {
                pointWidth: 4,
                events: {
                    legendItemClick: function () {

                        var peakChart = $("#ms1chart").highcharts();
                        var id = this.name;
                        $.each(peakChart.xAxis[0].plotLinesAndBands, function (index, el) {
                            if (el.id === id) {
                                el.svgElem[ el.visible ? 'show' : 'hide' ]();
                                el.visible = !el.visible;
                            }
                        });
                    }
                }
            },
            column: {
                grouping: false,
                shadow: false,
                borderWidth: 0,
                animation: false
            }
        },
        series: data.ms1Data.ms1PeaksData

    });
}

function updateChartMS2(data) {

    Highcharts.chart('ms2chart', {
        chart: {
            type: 'column',
            zoomType: 'x'
        },
        title: {
            text: data.ms2Data.titleMs2
        },
        subtitle: {
            text: data.ms2Data.subtitleMs2
        },
        xAxis: {
            min: data.ms2Data.minMZ_ms2,
            max: data.ms2Data.maxMZ_ms2,
            allowDecimals: true,
            title: {
                text: 'M/Z'
            },
            labels: {
                formatter: function () {
                    return this.value;
                }
            },
            plotBands: data.ms2Data.precursorBands
        },
        yAxis: {
            min: 0,
            title: {
                text: 'Intensity'
            },
            labels: {
                formatter: function () {
                    return this.value / 1000 + 'k';
                }
            }
        },
        /*exporting: {
            filename: 'MS2_' + data.ms2Data.searchID + '_' + data.ms2Data.peptideID,
            sourceWidth: 1280,
            sourceHeight: 720
        },*/
        credits: {
            enabled: false
        },
        legend: {
            enabled: true,
            borderWidth: 1,
            backgroundColor: (Highcharts.theme && Highcharts.theme.legendBackgroundColor) || '#FFFFFF'
        },
        tooltip: {
            formatter: function () {
                if (this.y === 0)
                    return false;
                else {
                    var pow = Math.pow(10, 3);
                    return '<b>MZ:</b> ' + Math.round(this.x * pow) / pow + '<br/><b>Intensity:</b> ' + Math.round(this.y);
                }
            }
        },
        plotOptions: {
            series: {
                dataLabels: {
                    enabled: true
                },
                pointWidth: 4,
                events: {
                    legendItemClick: function () {

                        var peakChart = $("#ms2chart").highcharts();
                        var id = this.name;
                        $.each(peakChart.yAxis[0].plotLinesAndBands, function (index, el) {

                            if (el.id === id) {
                                el.svgElem[ el.visible ? 'show' : 'hide' ]();
                                el.visible = !el.visible;
                            }
                        });
                    }
                }
            },
            column: {
                grouping: false,
                shadow: false,
                borderWidth: 0,
                animation: false
            }
        },
        series: data.ms2Data.ms2PeaksData

    });
}

function updateChartMSN(data) {
    Highcharts.chart('msnchart', {
        chart: {
            type: 'column',
            zoomType: 'x'
        },
        title: {
            text: data.msnData.titleMsN
        },
        subtitle: {
            text: data.msnData.subtitleMsN
        },
        xAxis: {
            min: data.msnData.minMZ_msN,
            max: data.msnData.maxMZ_msN,
            allowDecimals: true,
            title: {
                text: 'M/Z'
            },
            labels: {
                formatter: function () {
                    return this.value;
                }
            }
        },
        yAxis: {
            min: 0,
            title: {
                text: 'Intensity'
            },
            labels: {
                formatter: function () {
                    return this.value / 1000 + 'k';
                }
            },
            plotBands: data.msnData.noiseBand,
        },
        /*exporting: {
            filename: 'MSN_' + data.msnData.searchID + '_' + data.msnData.peptideID,
            sourceWidth: 1280,
            sourceHeight: 720
        },*/
        credits: {
            enabled: false
        },
        legend: {
            enabled: true,
            borderWidth: 1,
            backgroundColor: (Highcharts.theme && Highcharts.theme.legendBackgroundColor) || '#FFFFFF'
        },
        tooltip: {
            formatter: function () {
                if (this.y === 0)
                    return false;
                else {
                    var pow = Math.pow(10, 3);
                    return '<b>MZ:</b> ' + Math.round(this.x * pow) / pow + '<br/><b>Intensity:</b> ' + Math.round(this.y);
                }
            }
        },
        plotOptions: {
            series: {
                pointWidth: 4,
                events: {
                    legendItemClick: function () {

                        var peakChart = $("#msnchart").highcharts();
                        var id = this.name;
                        $.each(peakChart.yAxis[0].plotLinesAndBands, function (index, el) {

                            if (el.id === id) {
                                el.svgElem[ el.visible ? 'show' : 'hide' ]();
                                el.visible = !el.visible;
                            }
                        });
                    }

                }

            },
            column: {
                grouping: false,
                shadow: false,
                borderWidth: 0,
                animation: false
            }
        },
        series: data.msnData.msNPeaksData

    });
}

function getJsConnector() {
    return jsConnector;
}